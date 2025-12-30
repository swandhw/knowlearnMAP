서버 접속 정보 175.126.146.186
root / ef17c3F8

root 비번 수정해줘 => gustls88L!

사용자계정 생성 knowlearn / Elvlfoq@3

자꾸 물어 보지 말고 ssh key 로 작업해줘

local ip : 59.10.217.183

### 매우 중요 : 아래 혹시 로컬에서 개발 pg 와 arange 접속 가능하게 했더라도 로컬 아이피 지정하게 해줘

Linux Server Deployment Work Order
Objective: Set up a Dual Environment (Production/Development) for the KnowlearnMAP Project on a single Linux server. Executor: System Admin / AI Agent Key Requirement: Development Databases (PostgreSQL, ArangoDB) MUST be accessible from external networks (Local PC).

1. Directory Setup
Execute the following commands to create the necessary directory structure on the Linux host.

mkdir -p /home/knowlearn/project/config/nginx/conf.d
mkdir -p /home/knowlearn/project/data/prod/pg
mkdir -p /home/knowlearn/project/data/prod/arango
mkdir -p /home/knowlearn/project/data/dev/pg
mkdir -p /home/knowlearn/project/data/dev/arango
cd /home/knowlearn/project
2. Docker Compose Configuration
Create a file named docker-compose.yml in /home/knowlearn/project/.

Important Constraints Applied:

Dev PostgreSQL: Mapped to Host Port 5433 (Accessible Externally).
Dev ArangoDB: Mapped to Host Port 8530 (Accessible Externally).
Prod Services: Standard ports (5432, 8529).
Nginx: Ports 80, 443.
version: '3.8'
services:
  # ==========================================
  # Gateway Layer
  # ==========================================
  nginx-proxy:
    image: nginx:latest
    container_name: nginx-proxy
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./config/nginx/conf.d:/etc/nginx/conf.d
    depends_on:
      - app-prod
      - app-dev
    networks:
      - net-common
  # ==========================================
  # Production Environment
  # ==========================================
  app-prod:
    image: knowlearnmap:prod  # Replace with actual image
    container_name: app-prod
    environment:
      SPRING_PROFILES_ACTIVE: prod
      DB_HOST: pg-prod
      ARANGO_HOST: arango-prod
    networks:
      - net-common
    restart: always
  pg-prod:
    image: postgres:15
    container_name: pg-prod
    environment:
      POSTGRES_DB: knowlearn_prod
      POSTGRES_USER: admin
      POSTGRES_PASSWORD: prod_pg_password
    volumes:
      - ./data/prod/pg:/var/lib/postgresql/data
    networks:
      - net-common
    ports:
      - "5432:5432" # Optional: Restrict to 127.0.0.1:5432 if text-level security needed
  arango-prod:
    image: arangodb:latest
    container_name: arango-prod
    environment:
      ARANGO_ROOT_PASSWORD: prod_arango_password
    volumes:
      - ./data/prod/arango:/var/lib/arangodb3
    networks:
      - net-common
    ports:
      - "8529:8529"
  # ==========================================
  # Development Environment
  # ==========================================
  app-dev:
    image: knowlearnmap:dev  # Replace with actual image
    container_name: app-dev
    environment:
      SPRING_PROFILES_ACTIVE: dev
      DB_HOST: pg-dev
      ARANGO_HOST: arango-dev
    networks:
      - net-common
    restart: always
  pg-dev:
    image: postgres:15
    container_name: pg-dev
    environment:
      POSTGRES_DB: knowlearn_dev
      POSTGRES_USER: admin
      POSTGRES_PASSWORD: dev_pg_password
    volumes:
      - ./data/dev/pg:/var/lib/postgresql/data
    networks:
      - net-common
    ports:
      - "0.0.0.0:5433:5432" # EXPOSED: Connect via HostIP:5433
  arango-dev:
    image: arangodb:latest
    container_name: arango-dev
    environment:
      ARANGO_ROOT_PASSWORD: dev_arango_password
    volumes:
      - ./data/dev/arango:/var/lib/arangodb3
    networks:
      - net-common
    ports:
      - "0.0.0.0:8530:8529" # EXPOSED: Connect via HostIP:8530
networks:
  net-common:
    driver: bridge
3. Nginx Configuration
Create a file named default.conf in /home/knowlearn/project/config/nginx/conf.d/.

server {
    listen 80;
    server_name knowlearn_prod.com; # Replace with Production Domain
    location / {
        proxy_pass http://app-prod:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
server {
    listen 80;
    server_name dev.knowlearn_prod.com; # Replace with Dev Domain
    location / {
        proxy_pass http://app-dev:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
4. Execution Steps
Transfer Files: Ensure docker-compose.yml and default.conf are placed in the correct paths as defined above.
Pull Images: Ensure the application images (knowlearnmap:prod, knowlearnmap:dev) are present locally or in a registry.
# Example if using local build images
# docker load -i knowlearn_prod.tar
# docker load -i knowlearn_dev.tar
Start Services:
docker-compose up -d
Verify Status:
docker-compose ps
# All 7 containers should be 'Up'
5. Connection Check (Important)
Dev Postgres: Connect from local Windows IDE using Host_IP:5433 (User: admin, Pass: dev_pg_password).
Dev ArangoDB: Access web UI via http://Host_IP:8530 (User: root, Pass: dev_arango_password).


  완료 요약

  1. 방화벽 설정 (59.10.217.183에서만 DB 접속 허용)

  | 포트   | 서비스          | 접속 허용 IP  |
  |--------|-----------------|---------------|──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────
  | 5432   | Prod PostgreSQL | 59.10.217.183 |
  | 5433   | Dev PostgreSQL  | 59.10.217.183 |
  | 8529   | Prod ArangoDB   | 59.10.217.183 |──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────
  | 8530   | Dev ArangoDB    | 59.10.217.183 |                                                                                                              Auto-updating…
  | 80/443 | HTTP/HTTPS      | Anywhere      |
  | 22     | SSH             | Anywhere      |──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────
> 
  2. Nginx 도메인 설정──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────
  ? for shortcuts                                                                                                                                           Auto-updating…
  | 도메인              | 환경        | 프록시 대상   |
  |---------------------|-------------|---------------|
  | map.knowlearn.kr    | Production  | app-prod:8080 |
  | mapdev.knowlearn.kr | Development | app-dev:8080  |

  3. application.yml 프로필 추가

  | 프로필 | PostgreSQL           | ArangoDB             |
  |--------|----------------------|----------------------|
  | local  | 172.30.1.57:15433    | 172.30.1.57:8529     |
  | dev    | 175.126.146.186:5433 | 175.126.146.186:8530 |
  | prod   | 175.126.146.186:5432 | 175.126.146.186:8529 |

  DB 접속 정보

  Dev 환경:
  - PG: 175.126.146.186:5433 / admin / dev_pg_password / knowlearn_dev
  - Arango: 175.126.146.186:8530 / root / dev_arango_password

  Prod 환경:
  - PG: 175.126.146.186:5432 / admin / prod_pg_password / knowlearn_prod
  - Arango: 175.126.146.186:8529 / root / prod_arango_password

  참고: DNS에서 map.knowlearn.kr와 mapdev.knowlearn.kr를 175.126.146.186으로 설정해야 합니다.