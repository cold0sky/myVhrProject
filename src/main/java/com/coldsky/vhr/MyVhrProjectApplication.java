package com.coldsky.vhr;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.coldsky.vhr.Mapper")
@SpringBootApplication
public class MyVhrProjectApplication {

	public static void main(String[] args) {
		SpringApplication.run(MyVhrProjectApplication.class, args);
	}

}
