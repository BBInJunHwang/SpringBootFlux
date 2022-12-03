package com.spring.fluxdemo.domain;

import org.springframework.data.annotation.Id;

import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * https://spring.io/guides/gs/accessing-data-r2dbc/#initial 사이트 참고해서 domain, repository 가져오기
 * 
 * */


@RequiredArgsConstructor // final 붙어있는 생성자 생성
@Data
public class Customer {

    @Id
    private Long id;
    private final String firstName;
    private final String lastName;

}
