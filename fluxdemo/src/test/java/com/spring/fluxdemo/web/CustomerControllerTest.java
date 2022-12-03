package com.spring.fluxdemo.web;

import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import com.spring.fluxdemo.domain.Customer;
import com.spring.fluxdemo.domain.CustomerRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;



// 통합테스트용
//@SpringBootTest	// 스프링 통합테스트로 모든 필요한 빈 다 띄움 
//@AutoConfigureWebTestClient // 통테할떄 @WebFluxTest 이거 주석 처리시 해당 어노테이션 등록해야 webClient 사용 가능

@WebFluxTest	// ExtendWith 필요없음 해당 어노테이션이 다포함됨, @Controller 등 메모리에 띄워줌 단 @Service,@Component @Repository는 띄워주지 않음 
public class CustomerControllerTest {

	@Autowired
	private WebTestClient webClient;	// 비동기로 http 요청
										// webTestClient 가 DI 되는 이유는 @WebFluxTest 어노테이션 내 @AutoConfigureWebTestClient 포함되고 있어서 가능 

	/**
	 * @MockBean -> webflux 실행시 controller 띄울때 service 사용이 불가능 -> @WebFluxTest 는 @Service 안읽기 떄문 
	 * 이때 MockBean 사용하면 임시로 new 해서 띄워줌, @Import(Service.class) 동일하게 처리 가능하다.
	 * 
	 * @Import(repository.class) 하면 service와 다르게 사용이 불가능하다. -> interface 이기 때문, import는 인터페이스 등록 불가
	 * 그렇기 때문에 repository 빈으로 만들기 위해선 Mockbean 사용해야함 -> 인터페이스도 등록가능 
	 * 
	 * 하지만 MockBean은 임시 가짜 객체기 때문에 메소드 사용 불가
	 * @MockBean
	 * CustomerRepository customerRepository;
	 * public void 테스트(){
	 * 	customerRepository.findAll().log();	// 하지만 MockBean 은 가짜 임시객체여서 그렇기 때문에 findAll 등 메서드 사용 불가능 
	 * }
	 * 
	 * 그렇다고 @Autowired를 사용해도 Repository 못 읽음 
	 * CustomerRepository customerRepository;	// @MockBean 이용하면 임시객체여서 진짜 객체 DI 할려고 @Autowired 사용해도 @WebFluxTest는 @Repository 안읽음 어떻게 해겷할까?
	 * 
	 * 이떄는 @WebFluxTest 말고 @SpringBootTest 사용해서 모든 어노테이션 읽기 해야한다?? .. 근데 그건 통합테스트용이기 다띄우는거는 의미가 없는데.... 
	 * 
	 * */
	
// 근데 이렇게 테스트하면 모든걸 메모리에 띄우기 때문에 의미가 있나..????? 일단 따라해본다.
//	@Autowired
//	CustomerRepository customerRepository;
	
	@MockBean
	private CustomerRepository customerRepository;
	
	
	// test란 서버실행시 모든걸 IOC에 올리는게 아니라 필요한거만 올려서 test하는거다. 최대한 적게 띄워야함, 그렇지않으면 걍 서버 시작하는거랑 다를게없음
	@Test
	 public void 한건_테스트() {
		
		//given;
		Mono<Customer> givenData = Mono.just(new Customer("Jack", "Bauer"));
		
		// stub -> 행동지시
		// customerRepository 가 가짜객체여서 만약 findById(1L)가 실행이 되면 thenReturn 객체를 리턴해라 
		when(customerRepository.findById(1L)).thenReturn(givenData);
		
		
		webClient.get()
				 .uri("/customer/{id}",1L)	// {id} 값이 1 들어감
				 .exchange()
				 .expectBody()
				 .jsonPath("$.firstName").isEqualTo("Jack")
				 .jsonPath("$.lastName").isEqualTo("Bauer");

		// 테스트 정리하면 Controller를 테스트하는거며, @WebFluxTest 의해서 Controller가 메모리에 떴으며
		// 테스트가 시작되면 localhost:8080/customer/1L 경로가 호출될거고, return customerRepository.findById(id).log(); 실행될텐데
		// 실제로 customerRepository는 Mock 객체기 때문에 데이터를 못찾을거다.
		// 그렇기 떄문에 when..thenReturn 문으로 findById(1L) 호출시 givenData를 뱉을수 있도록 givenData 설정했다.
		// 이것의 궁극적인 목표는 repository로 데이터가 찾아지냐가 아닌, Controller를 테스트하는거다!
		
		
		
		
		
		
//		System.out.println("##################################################");
//		Flux<Customer> fCustomer = customerRepository.findAll();
//		fCustomer.subscribe(t->{
//			System.out.println("데이터");
//			System.out.println(t);
//		});
	 }
}
