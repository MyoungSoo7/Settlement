package github.lms.lemuel.order.application.port.out;

/**
 * 주문 생성 시 사용자 존재 여부 확인 Outbound Port
 * User 모듈과의 의존성을 인터페이스로 분리
 */
public interface LoadUserForOrderPort {

    boolean existsUser(Long userId);
}
