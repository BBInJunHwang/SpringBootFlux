package com.spring.fluxdemo.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.function.Predicate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;

import com.spring.fluxdemo.DBinit;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;


/**
 * 이렇게 테스트를 할때 필요한 객체만 스캔해서 띄우는 테스트를 진행해야한다
 * 
 * ex) controller만 띄울땐 @WebFluxTest 사용하고, R2db 만 테스트하고 싶으면 (repository 테스트만) @DataR2dbcTest 사용한다.
 * 하지만 DBinit 가 읽히진 않기 때문에 초기 데이터 적재는 되지 않는다. 
 * 그렇기 때문에 import(DBinit.class) 추가해주자 
 * 
 * */

@Import(DBinit.class)
@DataR2dbcTest	// 이것은 repository test 하기 위해서 사용하는 테스트로 mockbean 말고 진짜로 autowired로 테스트 가능 
public class CustomerRepositoryTest {

	@Autowired
	private CustomerRepository customerRepository;
	
	@Test
	public void 한건찾기테스트() {
		
		// 값 비교해보기.. 이거 문서 읽고 샘플 테스트하면서 찾아봐야함
		StepVerifier
		.create(customerRepository.findById(2L))
		.expectNextMatches((c)->{
			return c.getFirstName().equals("Chloe");
		})
		.expectComplete()
		.verify();
		
		//Mono<Customer> mCustomer = customerRepository.findById(2L)
		
//		
//		customerRepository.findById(1L).subscribe((c)->{
//			System.out.println(c);
//		});
	}
}
