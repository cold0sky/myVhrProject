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

### 配置用户名与密码

Spring Security有一个默认登录页面，页面代码是在jar包里的，默认的username是user，密码是随机生成的uuid格式的密码

要修改默认密码，可以在application.yml配置，也可以新建配置类

配置密码要用BCryptPasswordEncoder加密，不过登录还是明文

```java
@Configuration
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {    
        //auth.inMemoryAuthentication()
        auth.inMemoryAuthentication()
                .withUser("nicky")
                .password(bcryptPasswordEncoder().encode("123"))
                .roles("admin");
    }
	
	@Bean
    public PasswordEncoder bcryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

如果要数据库方式校验用户名密码，可以自定义UserDetailsService方式

```java
@Override
protected void configure(AuthenticationManagerBuilder auth) throws Exception {   
    auth.userDetailsService(userDetailsService)
        .passwordEncoder(new CustomPasswordEncoder());
    auth.parentAuthenticationManager(authenticationManagerBean());

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



