# 21. 이미지 업로드 컨벤션

## 개요

이미지 업로드는 **Presigned URL** 방식을 사용한다. 클라이언트가 서버로부터 Presigned URL을 발급받아 S3에 직접 업로드한다.

## 업로드 플로우

```
1. 클라이언트 → 서버: Presigned URL 발급 요청
2. 서버 → S3: Presigned URL 생성 (S3Presigner)
3. 서버 → 클라이언트: Presigned URL 반환
4. 클라이언트 → S3: Presigned URL로 이미지 직접 업로드
```

## 핵심 원칙

- 서버는 Presigned URL 발급만 담당하며, 이미지 바이너리를 직접 수신하지 않는다.
- S3 업로드는 클라이언트가 Presigned URL을 통해 직접 수행한다.
- AWS S3 설정은 `AwsProperties`, `AwsConfig`를 사용한다.
