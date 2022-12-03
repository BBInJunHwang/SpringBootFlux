package com.spring.fluxtest;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;


public class MyFilter implements Filter {
	
	private EventNotify eventNotify;
	public MyFilter(EventNotify eventNotify) {
		this.eventNotify = eventNotify;
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		System.out.println("필터1 시작됨");

		HttpServletResponse servletResponse = (HttpServletResponse) response; // 다운캐스팅
		//servletResponse.setContentType("text/plain;charset=utf-8"); // 한글꺠짐 방지 mimetype
		servletResponse.setContentType("text/event-stream;charset=utf-8"); // text/event-stream으로 변경
		PrintWriter out = servletResponse.getWriter();

		
		
		/**
		 * 분명 for문안에 out.flush() 를 시켜서 반복횟수만큼 flush를 시켜야하지만 
		 * mimetype text/plain 경우 웹 브라우저에서 flush가 되어도 잃지 않고 쌓아두다가 한방에 읽는다.
		 * 
		 * 하지만 text/event-stream 경우 for 갯수만큼 요청 및 flush 된다.
		 * 계속 브라우저가 재요청(돌면서) 응답받고 마지막차례가 오면 끝난다.
		 * 
		 * 이게 webflux 이며, 인터페이스 대로 개발하면됨
		 * 
		 * 
		 * */
		//지금은 테스트 이고 여기 응답 하는 부분은 reactive streams lib 쓰면 표준을 지켜서 응답 가능하다.
		// 백프래셔로 지정한 갯수만큼 주고 끝나면 종료
		for (int i = 0; i < 5; i++) {
			out.println("응답 : " + i);
			out.flush();	// 버퍼에 쌓인 데이터 비움
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		// 이떄 응답을 안끝내봄, 데이터 이어붙이기?
		/**
		 * localhost:8080/sse 호출시 
		 * 0
		 * 1
		 * 2
		 * 3
		 * 4
		 * 하고 무한 요청
		 * 
		 * 이때 localhost:8080/add 호출시
		 * 새로운 데이터 라는 출력이 생김 
		 * 
		 * 추가로 localhost:8080/sse 창을 하나 더 열고 /add 호출시 
		 * 두개 창 모두 새로운 데이터 출력이 생김
		 * 
		 * 이때 추가로 /sse 창을 열었을때 첫번째 /sse 창에 비하면 add 갯수가 마지막 데이터만 들고옴 -> hot 이라고 함, 전체 데이터를 다가져오기 위할떈 cold 개념을 써야함  
		 * 
		 * sse 프로토콜은 webflux 가 아닌 스프링 mvc에서도 동작은 가능하지만, 차이라면 mvc에서는 thread가 생성되고 webflux 에서는 단일 thread에서 이벤트 루프로 처리
		 * 
		 * 이렇게 로직짠건 맛보기용이고 실제로 reactive streams lib쓰면 매우 효율적으로 사용가능
		 * 
		 * 지금은 mvc에 thread 이용해서 하는건가?????
		 * 
		 * 실제 이렇게 짜면 플젝망함 ㅋㅋㅋ 그냥 개념적으로 이해하는 예시임
		 * 
		 * */
		// SSE Emitter lib 쓰면 편하게 쓸 수 있다.
		// 끝나지 않고 계속 연결
		while(true) {
			try {
				if(eventNotify.getChange()) {
					int lastIndex = eventNotify.getEvents().size() -1;
					out.println("응답 : " + eventNotify.getEvents().get(lastIndex));
					out.flush();
					eventNotify.setChange(false);
				}
				Thread.sleep(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		// webflux -> reactive streams 가 적용된 stream 을 배우고 (비동기 단일 스레드 동작)	-> 이게 효과적이다!
		// servlet mvc -> reactive streams 가 적용된 stream 을 배우고(멀티 스레드 방식)
		
		// 이거슨 선생님 부가설명
//		response 유지 가능 스펙
//		1. response를 유지하는 것은 ServletMVC도 가능하다. 꼭 WebFlux만 되는 것은 아니다.
//		2. 다만 멀티스레드에서 response가 계속 유지되면 스레드 종료가 되지 않기 때문에 트래픽이 많아진다.
//		3. 단일 스레드에서 비동기로 사용한다면 더 효율적으로 사용 가능하다.
//
//		response 유지 범위
//		1. response를 유지할 때 데이터의 크기만큼만 지속적으로 응답하고 종료되는 방식이 있다.
//		2. response를 유지할 때 무한으로 유지하는 방식이 있다.
//
//		response 응답 방식
//		1. 데이터의 크기만큼만 응답을 하는 것은 Flux라고 한다.
//		2. 무한으로 응답하는 방식을 SSE 라고 한다.
//
//		reactive-streams
//		1. 해당 라이브러리를 사용하면 Flux방식으로 응답시에 백프레셔가 적용가능해진다. 데이터가 100건이 있는데 Flux방식으로 리턴하게 되면 한번에 flush를 몇개씩 할지를 정할 수 있다. 그렇게 되면 데이터를 소비하는 쪽에서 처리할 수 있을 만큼의 데이터를 순차적으로 응답할 수 있다. 가령 10개씩 처리할 수 있으면 10개씩 flush하면 된다. 이걸 백프레셔(역압)이라고 한다.
//		2. 해당 라이브러리를 사용하면 4가지 표준 스팩을 제공해주는데 그 스팩으로코드를 짜면 편하게 구현할 수 있다.
//
//		WebFlux
//		1. reactive-streams의 구현체이다.
//		2. 단일스레드 비동기 방식이다.
//		3. Flux를 쉽게 사용할 수 있다.
//		4. 물론 SSE도 적용하면 사용가능하다.
	}
}
