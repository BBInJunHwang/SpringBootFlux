package com.spring.fluxtest;

import javax.servlet.Filter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.AbstractFilterRegistrationBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MyFilterConfig {
	
	@Autowired
	private EventNotify eventNotify;

	@Bean
	public AbstractFilterRegistrationBean<Filter> addFilter(){
		System.out.println("필터1 등록됨");
		FilterRegistrationBean<Filter> bean = new FilterRegistrationBean<>(new MyFilter(eventNotify));
		bean.addUrlPatterns("/sse"); 
		
		return bean;
	}
	
	@Bean
	public AbstractFilterRegistrationBean<Filter> addFilter2(){
		System.out.println("필터2 등록됨");
		FilterRegistrationBean<Filter> bean = new FilterRegistrationBean<>(new MyFilter2(eventNotify));
		bean.addUrlPatterns("/add"); 
		
		return bean;
	}
}
