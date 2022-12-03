package com.spring.fluxdemo.web;

import java.time.Duration;

import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.spring.fluxdemo.domain.Customer;
import com.spring.fluxdemo.domain.CustomerRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

@RestController
public class CustomerController {

	private final CustomerRepository customerRepository;
	private final Sinks.Many<Customer> sink; // sink란 A요청이 있고 B요청이 있는경우
											 // A요청 -> Flux -> Stream
											 // B요청 -> Flux -> Stream 
											 // 2개의 Stream을 merge 한다. -> flux.merge -> 싱크가 맞춰진다.
	
	public CustomerController(CustomerRepository customerRepository) {
		this.customerRepository = customerRepository;
		sink = Sinks.many().multicast().onBackpressureBuffer();	// multicast 새롭게 push 된 데이터만 구독자에게 전송, unicast 는 버퍼링되기전에 왜곡됨, replay 방식.. 등  
																// 모든 client flux 요청을 sink한다? 
	}
	
	
	
	@GetMapping("/flux")
	public Flux<Integer> flux(){
		//return Flux.just(1,2,3,4,5);	// just는 데이터를 subscribe 해서 순차적으로 onNext하면서 꺼내준다.
		return Flux.just(1,2,3,4,5).delayElements(Duration.ofSeconds(1)).log();	// 1초마다 delay하면서 데이터 onNext한다.	브라우져에 호출시 계속 돌다가 모아놓고 5초뒤에 한방에 데이터 반환함
	}
	
	@GetMapping(value = "fluxstream", produces = MediaType.APPLICATION_STREAM_JSON_VALUE)	// MediaType Stream을 주게 되면 onNext 할때마다 하나씩 버퍼를 flush 해줌
	public Flux<Integer> fluxstream(){														// response가 유지되면서 계속 하나씩 받는다.
		return Flux.just(1,2,3,4,5).delayElements(Duration.ofSeconds(1)).log();
	}
	
	
	
	@GetMapping("/customer/{id}")
	public Mono<Customer> findById(@PathVariable Long id){	// 이렇게 한건으로 데이터를 return 시 Mono로 하나만 반환 onNext  한방에 끝낸다는 의미
		return customerRepository.findById(id).log();
	}
	
	
	//http://localhost:8080/customer
	@GetMapping(value = "/customer", produces = MediaType.APPLICATION_STREAM_JSON_VALUE)
	public Flux<Customer> findAll(){
		return customerRepository.findAll().delayElements(Duration.ofSeconds(1)).log();		// return 되면서 로그가 남음
	}
	
	
	// sse는 연결이 계속 유지된다. stream 데이터 다 끝나면 종료되는게 아님 
	//@GetMapping(value = "/customer/sse" , produces = MediaType.TEXT_EVENT_STREAM_VALUE)	// text/event-stream mimeType 주게되면  
	//	public Flux<Customer> findAllSSE(){													// data:{"id":1,"firstName":"Jack","lastName":"Bauer"} 이런구조로 key값으로 data가 붙는다.			
	//		return customerRepository.findAll().delayElements(Duration.ofSeconds(1)).log();	
	//}
	
	
	@GetMapping(value = "/customer/sse")						// ServerSentEvent 타입으로 보내게 되면 produces = MediaType.TEXT_EVENT_STREAM_VALUE 이거 생략 가능
		public Flux<ServerSentEvent<Customer>> findAllSSE(){
			//return customerRepository.findAll().delayElements(Duration.ofSeconds(1)).log();	
			
		//return sink.asFlux().map(c-> ServerSentEvent.builder(c).build());	// return 타입이 ServerSentEvent 타입
		return sink.asFlux().map(c-> ServerSentEvent.builder(c).build()).doOnCancel(()->{
			sink.asFlux().blockLast();		// 만약 요청을 하다가 끊었으면 재요청이 불가능하다. sink쪽에서 끊었는지 알 수가 없기 때문 
											// 만약 끊었을떄 (cancel 요청 들어올떄 doOnCancel) flux 의 마지막 데이터를 하나 날려준다(강제로 마지막 데이터 날려줌)
		});
		
	/**
	 * 홍은 -> A flux (1,2,3 구독)
	 * 임은 -> B flux (3,4,5 구독)
	 * 
	 * A,B 두개의 flux를 merge 해서 새로운 sink를 만들 수 있다.
	 * sink 시 1,2,3,3,4,5 생성 
	 * 이떄 홍이 6데이터 push 함 -> tryEmitNext
	 * 이때 임은 sink 데이터에 새로운 값이 들어오는지 확인하고 있다.
	 * 
	 * 
	 * */
	
	}
	
	
	
	@PostMapping("/customer")
	public Mono<Customer> save(){
		return customerRepository.save(new Customer("gildong", "Hong")).doOnNext(c -> {
			sink.tryEmitNext(c);	// pub 데이터 추가한다.	위에 customer/sse 요청에 sink 부분에 stream을 추가하면서 데이터가 이어붙게된다.
		});
	}
	
	
	
	
	//http://localhost:8080/customer
//	@GetMapping("/customer")
//	public Flux<Customer> findAll(){
//		return customerRepository.findAll().delayElements(Duration.ofSeconds(1)).log();		// return 되면서 로그가 남음
//	}
// 	 http://localhost:8080/customer 실행 시 아래와 같이 로그가 남는다.
//	
//	 onSubscribe(FluxUsingWhen.UsingWhenSubscriber)				-> DB 데이터를 구독한다, 응답이된다. 구독정보 돌려줄게~
//	 request(unbounded)											-> unbounded는 한번에 가진 데이터 다줘라, 만약 1이면 한개씩 받는다. max 의미
//	 onNext(Customer(id=1, firstName=Jack, lastName=Bauer))
//	 onNext(Customer(id=2, firstName=Chloe, lastName=O'Brian))
//	 onNext(Customer(id=3, firstName=Kim, lastName=Bauer))
//	 onNext(Customer(id=4, firstName=David, lastName=Palmer))
//	 onNext(Customer(id=5, firstName=Michelle, lastName=Dessler))
//	 onComplete()												-> onNext 쭉 실행되고 complete 로 응답된다.
	
}
