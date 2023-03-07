## 数据库设计

主要包含了五张表，分别是

- 资源表
- 角色表
- 用户表
- 资源角色表
- 用户角色表



![p274](http://img.itboyhub.com/2020/04/vhr/p274.png)

1. hr 表是用户表，存放了用户的基本信息。
2. role 是角色表，name字段表示角色的英文名称，按照 SpringSecurity 的规范，将以 `ROLE_` 开始，nameZh 字段表示角色的中文名称。
3. menu 表是一个资源表，该表涉及到的字段有点多，由于我的前端采用了 Vue 来做，因此当用户登录成功之后，系统将根据用户的角色动态加载需要的模块，所有模块的信息将保存在 menu 表中，menu 表中的 path、component、iconCls、keepAlive、requireAuth 等字段都是 Vue-Router 中需要的字段，也就是说 menu 中的数据到时候会以 json 的形式返回给前端，再由 vue 动态更新 router，menu 中还有一个字段 url，表示一个 url pattern，即路径匹配规则，假设有一个路径匹配规则为 `/admin/**`,那么当用户在客户端发起一个 `/admin/user` 的请求，将被 `/admin/**` 拦截到，系统再去查看这个规则对应的角色是哪些，然后再去查看该用户是否具备相应的角色，进而判断该请求是否合法。
4. role和hr、menu和role都是多对多的关系，因此增设两个关系表。

## Spring Security

### 介绍

Spring Security 是 Spring 家族中的一个安全管理框架，应用程序的两个主要区域是“认证”和“授权”（或者访问控制）。Spring Security是针对Spring项目的安全框架，也是Spring Boot底层安全模块默认的技术选型

这两个主要区域是Spring Security 的两个目标。

- “认证”（Authentication），是建立一个他声明的主体的过程（一 个“主体”一般是指用户，设备或一些可以在你的应用程序中执行动 作的其他系统）。

- “授权”（Authorization）指确定一个主体是否允许在你的应用程序 执行一个动作的过程。为了抵达需要授权的店，主体的身份已经有认 证过程建立。

  ![](https://sslstatic.ktanx.com/images/release/201648/viVrX8eqCaUUgnhW.jpg)

### 权限控制基本功能实现

#### 前置步骤

1. 系统权限设计

   设计多个不同的页面，并进行权限区分

2. 创建数据库表，添加用户数据

3. 建立用户对象

   创建User类

4. 建立用户数据层

   创建UserMapper接口和UserMapper.xml

5. 建立页面跳转控制类

   创建UserController类

6. 添加权限验证

   到上面那一步，系统功能已经差不多了，但是还缺少权限验证的配置。

   其实权限控制从pom.xml添加`spring-boot-starter-security`依赖开始就已经起作用了，

   如果这时候启动项目访问的话，会发现`Spring Security`已经将所有请求拦截并自动生成了一个登录框让你登录。

   但显然这个登录框你是无法登录成功的，因为后台具体登录的逻辑我们还没有完成。

   


#### 建立自定义的UserDetailsService

`Spring Security`的用户信息获取最终是通过`UserDetailsService`的`loadUserByUsername`方法来完成的

根据上面的`UserMapper`实现，我们建立自定义的`CustomUserDetailService`。

```java
public class CustomUserDetailsService implements UserDetailsService {
	    @Autowired
    HrMapper hrMapper;


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Hr hr = hrMapper.loadUserByUsername(username);
        if (hr == null) {
            throw new UsernameNotFoundException("用户名不存在");
        }
        return hr;
    }

}
```



#### 配置Security

接下来就是配置`Spring Security`了，我们建立一个类`SecurityConf`，使用`JavaConfig`的方式，指定`AuthenticationManager`使用我们自己的`CustomUserDetailsService`来获取用户信息，并设置首页、登录页等。

```java
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConf extends WebSecurityConfigurerAdapter {

    @Bean
    public UserDetailsService userDetailsService() {
        return new CustomUserDetailsService();
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService());
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .authorizeRequests()
                .antMatchers("/", "/index")
                .permitAll()
                .anyRequest()
                .authenticated()
                .and()
                .formLogin()
                .loginPage("/login")
                .defaultSuccessUrl("/user-page")
                .permitAll()
                .and()
                .logout()
                .permitAll();
    }
}
```

可以看到`SecurityConf`上添加了`@EnableWebSecurity`注解用来跟`Spring mvc`集成。同时它还继承了`WebSecurityConfigurerAdapter`类用来重写我们需要的配置。



#### 添加角色权限验证

上面已经完成了系统的登录和验证功能，但并没有进行权限的区分，要怎么样把普通用户和管理用户区分开呢？

很简单，只需要增加`@PreAuthorize`注解即可。修改`UserController`，对`/user`和`/admin`分别添加注解：

```java
@RequestMapping(value = "/user", method = RequestMethod.GET)
@PreAuthorize("hasAnyRole('admin', 'user')")
public String userPage() {
    return "user-page";
}

@RequestMapping(value = "/admin", method = RequestMethod.GET)
@PreAuthorize("hasAnyRole('admin')")
public String adminPage() {
    return "admin-page";
}
```



### 自定义决策管理器(动态权限码)

#### 权限资源 SecurityMetadataSource

要实现动态的权限验证，当然要先有对应的访问权限资源了。`Spring Security`是通过`SecurityMetadataSource`来加载访问时所需要的具体权限，所以第一步需要实现`SecurityMetadataSource`。

`SecurityMetadataSource`是一个接口，同时还有一个接口`FilterInvocationSecurityMetadataSource`继承于它，但`FilterInvocationSecurityMetadataSource`只是一个标识接口，对应于`FilterInvocation`，本身并无任何内容：

```java
/**
 * Marker interface for <code>SecurityMetadataSource</code> implementations that are
 * designed to perform lookups keyed on {@link FilterInvocation}s.
 *
 * @author Ben Alex
 */
public interface FilterInvocationSecurityMetadataSource extends SecurityMetadataSource {
}
```

因为我们做的一般都是web项目，所以实际需要实现的接口是`FilterInvocationSecurityMetadataSource`，这是因为`Spring Security`中很多web类使用的类参数类型都是`FilterInvocationSecurityMetadataSource`。

下面是一个自定义实现类`CustomSecurityMetadataSource`的示例代码，它的主要责任就是当访问一个`url`时返回这个url所需要的访问权限。

```java
public class CustomSecurityMetadataSource implements FilterInvocationSecurityMetadataSource {

    // 返回本次访问需要的权限，可以有多个权限
    // 没有匹配的url直接返回null,即设置为白名单
    public Collection<ConfigAttribute> getAttributes(Object object) throws IllegalArgumentException {

        FilterInvocation fi = (FilterInvocation) object;

        Map<String, Collection<ConfigAttribute>> metadataSource = CustomSecurityContext.getMetadataSource();

        for (Map.Entry<String, Collection<ConfigAttribute>> entry : metadataSource.entrySet()) {
            String uri = entry.getKey();
            RequestMatcher requestMatcher = new AntPathRequestMatcher(uri);
            if (requestMatcher.matches(fi.getHttpRequest())) {
                return entry.getValue();
            }
        }

        return null;
    }
	
    // 返回了所有定义的权限资源,Spring Security会在启动时校验。
    // 不需要校验直接返回null
    public Collection<ConfigAttribute> getAllConfigAttributes() {
        return null;
    }

    public boolean supports(Class<?> clazz) {
        return FilterInvocation.class.isAssignableFrom(clazz);
    }
}
```

`getAttributes`方法返回本次访问需要的权限，可以有多个权限。在上面的实现中如果没有匹配的url直接返回null，也就是没有配置权限的url默认都为`白名单`，想要换成默认是`黑名单`只要修改这里即可。

`getAllConfigAttributes`方法如果返回了所有定义的权限资源，`Spring Security`会在启动时校验每个`ConfigAttribute`是否配置正确，不需要校验直接返回null。

`supports`方法返回类对象是否支持校验，web项目一般使用`FilterInvocation`来判断，或者直接返回true。

在上面我们主要定义了两个权限码：

```properties
user=/user
admin=/admin,/user
```

也就是`CustomSecurityContext.getMetadataSource()`加载的内容，主要加载代码如下：

```java
// 加载所有配置文件信息
ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
Resource[] resources = resourcePatternResolver.getResources("classpath*:/security/*.properties");
if (ArrayUtils.isEmpty(resources)) {
    return;
}

// 所有配置信息写入properties对象
Properties properties = new Properties();
for (Resource resource : resources) {
    properties.load(resource.getInputStream());
}

// 遍历properties对象，对配置数据进行装配
for (Map.Entry<Object, Object> entry : properties.entrySet()) {

    String key = (String) entry.getKey();
    String value = (String) entry.getValue();

    String[] values = StringUtils.split(value, ",");

    for (String v : values) {
        if (!METADATA_SOURCE_MAP.containsKey(StringUtils.trim(v))) {
            METADATA_SOURCE_MAP.put(StringUtils.trim(v), new ArrayList<ConfigAttribute>());
        }
        METADATA_SOURCE_MAP.get(StringUtils.trim(v)).add(new SecurityConfig(key));
    }
}
```

这里我们把权限的配置信息写在了`properties`文件中，当然你也可以存在数据库中或其它任何地方。

在加载的时候，这里的`url`是key，`value`是访问需要的权限码，一个权限码可以对应多个url，一个url也可以有多个权限码，想要怎么玩都可以在这里实现，示例中只是最简单的。

#### 权限决策 AccessDecisionManager

有了权限资源，知道了当前访问的url需要的具体权限，接下来就是决策当前的访问是否能通过权限验证了。

这需要通过实现自定义的`AccessDecisionManager`来实现。`Spring Security`内置的几个`AccessDecisionManager`就不讲了，在web项目中基本用不到。

以下是示例代码：

```java
public class CustomAccessDecisionManager implements AccessDecisionManager {

    public void decide(Authentication authentication, Object object, Collection<ConfigAttribute> configAttributes) throws AccessDeniedException, InsufficientAuthenticationException {

        Iterator<ConfigAttribute> iterator = configAttributes.iterator();
        while (iterator.hasNext()) {

            if (authentication == null) {
                throw new AccessDeniedException("当前访问没有权限");
            }

            ConfigAttribute configAttribute = iterator.next();
            String needCode = configAttribute.getAttribute();

            Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
            for (GrantedAuthority authority : authorities) {
                if (StringUtils.equals(authority.getAuthority(), "ROLE_" + needCode)) {
                    return;
                }
            }
        }

        throw new AccessDeniedException("当前访问没有权限");
    }

    public boolean supports(ConfigAttribute attribute) {
        return true;
    }

    public boolean supports(Class<?> clazz) {
        return FilterInvocation.class.isAssignableFrom(clazz);
    }
}
```

同样的也有三个方法，其它两个和`SecurityMetadataSource`类似，这里主要讲讲`decide`方法。

`decide`方法的三个参数中：

- `authentication`包含了当前的用户信息，包括拥有的权限。这里的权限来源就是前面登录时`UserDetailsService`中设置的`authorities`。
- `object`就是`FilterInvocation`对象，可以得到`request`等web资源。
- `configAttributes`是本次访问需要的权限。

上面的实现中，当需要多个权限时只要有一个符合则校验通过，即`或`的关系，想要`并`的关系只需要修改这里的逻辑即可。

#### 配置使用自定义实现类

上面权限的资源和验证我们已经都实现了，接下来就是指定让`Spring Security`使用我们自定义的实现类了。

在以前`xml`的配置中，一般都是自己实现一个`FilterSecurityInterceptor`，然后注入自定义的`SecurityMetadataSource`和`AccessDecisionManager`，就像下面这样：

```xml
<b:bean id="customFilterSecurityInterceptor" class="com.dexcoder.security.CustomFilterSecurityInterceptor">  
        <b:property name="authenticationManager" ref="customAuthenticationManager" />  
        <b:property name="accessDecisionManager" ref="customAccessDecisionManager" />  
        <b:property name="securityMetadataSource" ref="customSecurityMetadataSource" />  
</b:bean> 
```

在`Spring Boot`的`JavaConfig`中并没有这样的实现方式，但是提供了`ObjectPostProcessor`以让用户实现更多想要的高级配置。具体看下面代码，注意`withObjectPostProcessor`部分：

```java
@Bean
public AccessDecisionManager accessDecisionManager() {
    return new CustomAccessDecisionManager();
}

@Bean
public FilterInvocationSecurityMetadataSource securityMetadataSource() {
    return new CustomSecurityMetadataSource();
}


protected void configure(HttpSecurity http) throws Exception {
    http.authorizeRequests().antMatchers("/", "/index").permitAll().anyRequest().authenticated().and().formLogin()
            .loginPage("/login").defaultSuccessUrl("/user").permitAll().and().logout().permitAll()

            .and().authorizeRequests().anyRequest().authenticated()
            .withObjectPostProcessor(new ObjectPostProcessor<FilterSecurityInterceptor>() {

        public <O extends FilterSecurityInterceptor> O postProcess(O fsi) {
            fsi.setAccessDecisionManager(accessDecisionManager());
            fsi.setSecurityMetadataSource(securityMetadataSource());
            return fsi;
        }
    });
}
```

主要是在创建默认的`FilterSecurityInterceptor`的时候把我们的`accessDecisionManager`和`securityMetadataSource`设置进去，至于`authenticationManager`因为我们已经声明了`authenticationProvider`并设置了`userDetailService`，所以这里可以省去。

既然扯到了`FilterSecurityInterceptor`这里再唠叨两句，`Spring Security`内部默认主要有三个实现，见下图：

![Spring Security Interceptor](https://sslstatic.ktanx.com/images/release/201650/BU9lWJd3D9gFVQne.png)

`AspectJMethodSecurityInterceptor`和`MethodSecurityInterceptor`在`spring-security-core`包内，`FilterSecurityInterceptor`在`spring-security-web`包内，这也说明`FilterSecurityInterceptor`是web项目专用的。

在前面默认的实现中，`Controller`上加注解`@PreAuthorize`使用的是`MethodSecurityInterceptor`，但是在经过我们一番改造后，已经使用了`FilterSecurityInterceptor`，`MethodSecurityInterceptor`已经没用到了。

当然你需要把`Controller`方法上的注解去掉：

```java
@RequestMapping(value = "/admin", method = RequestMethod.GET)
//    @PreAuthorize("hasAnyRole('admin')")
public String helloAdmin() {
    return "admin";
}

@RequestMapping(value = "/user", method = RequestMethod.GET)
//    @PreAuthorize("hasAnyRole('admin','user')")
public String helloUser() {
    return "user";
}
```

## 类

### Entity

Hr

HrRole

Menu

MenuRole

Role

### Service



### Controller

UrlFilterInvocationSecurityMetadataSource 

实现FilterInvocationSecurityMetadataSource 接口，用于获取界面的权限信息。



UrlAccessDecisionManager

实现 AccessDecisionManager 接口，用于自定义的权限决策。



AuthenticationAccessDeniedHandler

实现 AccessDeniedHandler 接口，用于发送权限错误时显示错误信息