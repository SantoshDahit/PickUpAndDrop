# 17. SMS 인증 컨벤션

## 개요

본 프로젝트는 **SMS 인증 코드 검증 시스템**을 사용합니다.
`SmsSender` 인터페이스 기반의 Strategy 패턴으로, 현재 Solapi를 구현체로 사용합니다.
인증 코드는 3단계 상태 전이(PENDING → VERIFIED → USED)를 거칩니다.

---

## 아키텍처

```
Controller
  ↓
SmsVerificationFacade
  ├── SmsSender (인터페이스)
  │   └── SolapiSmsSender (구현체)
  └── SmsVerificationService
        └── SmsVerificationRepository
              └── SmsVerification (엔티티)
```

### Strategy 패턴

```java
// SmsSender.java - 인터페이스
public interface SmsSender {
    void sendSms(String phoneNumber, String message);
}

// SolapiSmsSender.java - 구현체
@Component
public class SolapiSmsSender implements SmsSender {
    @Override
    public void sendSms(String contact, String messageContent) {
        Message message = new Message();
        message.setFrom(senderNumber);
        message.setTo(contact);
        message.setText(messageContent);
        messageService.send(message);
    }
}
```

> SMS 발송 서비스 교체 시 `SmsSender` 인터페이스의 새 구현체만 추가하면 됩니다.

---

## SmsVerification 엔티티

```java
// SmsVerification.java
@Entity
@Table(name = "sms_verification")
public class SmsVerification extends BaseTimeEntity {

    @Id
    @Column(updatable = false, nullable = false)
    private String id;

    @Column(name = "contact", nullable = false)
    private String contact;

    @Column(name = "pin", nullable = false)
    private String pin;                        // 4자리 인증 코드

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false)
    private SmsPurpose purpose;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SmsStatus status;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "expired_at", nullable = false)
    private LocalDateTime expiredAt;           // 생성 시점 + 10분

    public SmsVerification(String contact, String pin, SmsPurpose purpose) {
        this.id = UUID.randomUUID().toString();
        this.contact = contact;
        this.pin = pin;
        this.purpose = purpose;
        this.status = SmsStatus.PENDING;
        this.expiredAt = LocalDateTime.now().plusMinutes(10);  // 10분 유효
    }

    public void updateSmsVerificationStatus(SmsStatus status) {
        if (SmsStatus.VERIFIED.equals(status)) {
            this.verifiedAt = LocalDateTime.now();
        }
        this.status = status;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiredAt);
    }
}
```

### 핵심 필드

| 필드 | 타입 | 설명 |
|------|------|------|
| `id` | `String (UUID)` | PK |
| `contact` | `String` | 전화번호 (하이픈 제거된 형태) |
| `pin` | `String` | 4자리 인증 코드 (1000~9999) |
| `purpose` | `SmsPurpose` | 인증 목적 |
| `status` | `SmsStatus` | 현재 상태 |
| `verifiedAt` | `LocalDateTime` | 인증 완료 시각 (VERIFIED 전환 시) |
| `expiredAt` | `LocalDateTime` | 만료 시각 (생성 시점 + 10분) |

---

## PIN 생성

```java
// SmsVerificationFacade.java
private String generateRandomPin() {
    return String.valueOf((int) (Math.random() * AuthConstants.PIN_RANGE + AuthConstants.PIN_MIN_VALUE));
}
```

- `AuthConstants.PIN_MIN_VALUE = 1000`
- `AuthConstants.PIN_RANGE = 9000`
- **결과**: 1000 ~ 9999 사이의 4자리 숫자

---

## 인증 플로우 (3단계)

### 1단계: Create (PENDING)

```
POST /v1/sms-verifications
  ↓
SmsVerificationFacade.create()
  ├── validateContact() → 연락처 유효성 검증
  ├── generateRandomPin() → 4자리 PIN 생성
  ├── contact 정규화 (하이픈 제거)
  ├── smsSender.sendSms() → SMS 발송
  │   메시지: "[서비스명] verification pin: {PIN}"
  └── smsVerificationService.create() → PENDING 상태로 저장
      └── expiredAt = now() + 10분
```

### 2단계: Verify (VERIFIED)

```
POST /v1/sms-verifications/verify
  ↓
SmsVerificationService.verifyCodeAndMarkAsVerified()
  ├── isExpired() → 만료 체크 (SMS_CODE_EXPIRED)
  ├── contact 비교 → 연락처 일치 확인 (CONTACT_MIS_MATCH)
  ├── pin 비교 → PIN 일치 확인 (SMS_PIN_NOT_VERIFIED)
  ├── purpose 비교 → 목적 일치 확인 (SMS_PURPOSE_INVALID)
  └── updateSmsVerificationStatus(VERIFIED) → verifiedAt 기록
```

### 3단계: Use (USED)

```
(회원가입, 비밀번호 변경 등 최종 동작)
  ↓
SmsVerificationService.validateAndMarkAsUsed()
  ├── status == VERIFIED 확인 (SMS_STATUS_IS_NOT_VERIFY)
  ├── contact 일치 확인 (CONTACT_MIS_MATCH)
  ├── purpose 일치 확인 (SMS_PURPOSE_INVALID)
  └── updateSmsVerificationStatus(USED)
```

### 상태 전이 다이어그램

```
PENDING → VERIFIED → USED
            │
            └── (만료 시) EXPIRED
```

---

## SmsPurpose enum

```java
// SmsPurpose.java
public enum SmsPurpose {
    JOIN,              // 회원가입
    PASSWORD_REST,     // 비밀번호 재설정
    DELETE_ACCOUNT,    // 계정 삭제
    VERIFY_ACCOUNT     // 계정 인증
}
```

---

## SmsStatus enum

```java
// SmsStatus.java
public enum SmsStatus {
    PENDING,    // SMS 발송됨, 미인증
    VERIFIED,   // PIN 인증 완료
    USED,       // 최종 사용 완료
    EXPIRED     // 만료 (10분 초과)
}
```

---

## 에러 코드

| ErrorCode | 설명 |
|-----------|------|
| `SMS_SEND_FAIL` | SMS 발송 실패 (Solapi 오류) |
| `SMS_CODE_EXPIRED` | 인증 코드 만료 (10분 초과) |
| `SMS_PIN_NOT_VERIFIED` | PIN 불일치 |
| `SMS_STATUS_IS_NOT_VERIFY` | 상태가 VERIFIED가 아님 (Use 단계 실패) |
| `SMS_PURPOSE_INVALID` | 인증 목적 불일치 |
| `SMS_VERIFICATION_IS_NOT_FOUND` | 인증 레코드 없음 |
| `SMS_VERIFICATION_ID_IS_NULL` | 인증 ID null |
| `CONTACT_IS_INVALID` | 연락처 형식 오류 |
| `CONTACT_MIS_MATCH` | 연락처 불일치 |

---

## 설정 (application.yml)

```yaml
# application-common.yml
solapi:
  api:
    key: ${SOLAPI_KEY}
    secret: ${SOLAPI_SECRET}
    senderNumber: "15517381"
    url: "https://api.solapi.com/messages/v4/send"
```

### SolapiSmsSender 초기화

```java
// SolapiSmsSender.java
@PostConstruct
public void init() {
    this.messageService = NurigoApp.INSTANCE.initialize(
            apiKey, apiSecret, "https://api.solapi.com"
    );
}
```

---

## 비밀번호 재설정 특수 흐름

비밀번호 재설정 시에는 `VERIFY_ACCOUNT` 목적으로 이미 `USED` 상태인 인증도 허용합니다.

```java
// SmsVerificationService.java
public void validateAndMarkAsUsedByPasswordReset(String smsVerificationId, String contact) {
    if (verification.getPurpose().equals(SmsPurpose.PASSWORD_REST)) {
        // VERIFIED 상태 필요
    } else if (verification.getPurpose().equals(SmsPurpose.VERIFY_ACCOUNT)) {
        // USED 상태도 허용
    }
}
```
