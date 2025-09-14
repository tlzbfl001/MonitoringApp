# MonitoringApp

Kotlin 기반의 **모니터링 애플리케이션** 프로젝트입니다. 백엔드 서버를 활용한 센서데이터 모니터링 앱입니다.

---

##  주요 기능
- ESP32를 이용한 IoT 센서 데이터 수집 시스템(호흡량, 낙상 횟수, 활동량 등)
- 실시간 또는 주기별 모니터링 데이터 제공
- REST API 기반 상태 조회 (`/health`, `/auth`)
- 알림 기능(특정 임계치 초과 시 경고)
- 대시보드 시각화, 그래프 표시 UI 

---

##  기술 스택
- **언어**: Kotlin
- **빌드 도구**: Gradle (Kotlin DSL)
- **CI/CD**: Jenkins 
- 데이터베이스: PostgreSQL / MySQL
- API 문서화: Swagger/OpenAPI

---

##  실행 & 설치 가이드
1. 리포지토리 클론:
   ```bash
   git clone https://github.com/yej431/MonitoringApp.git
   cd MonitoringApp
