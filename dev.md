# Real-Time Board Manager – Internal Development Notes

Real-time board management system designed to support dynamic team workflows with real-time updates, secure user authentication, and efficient notification handling.  
Built using a modern backend stack, the system enables users to create boards, manage tasks, and receive instant updates and alerts, making it ideal for agile teams and internal project coordination.  
Main goal is learning how to work with WebSockets, Redis cache, and brokers like RabbitMQ.

## 🧱 Tech Stack
- Java 21
- Spring Boot
- MySQL (main database)
- Redis (caching only)
- RabbitMQ (notifications + event queue)
- WebSocket (real-time updates)
- JWT (authentication)
- Docker (containerization)
- Swagger (API documentation)
- Lombok (boilerplate reduction)

## 📁 Project Structure (Initial)
src/
├── controller/
├── service/
├── model/
├── repository/
├── config/
├── dto/
└── websocket/

## 🔐 Authentication
- JWT-based login and registration
- Spring Security configuration
- User roles: ADMIN, MEMBER

## 🗃️ Database Entities
- User: id, username, email, password, role
- Board: id, name, owner_id, created_at
- Task: id, title, description, status, board_id, assigned_to, timestamps
- TaskHistory: id, task_id, changed_by, change_type, timestamp

## 🔄 Real-Time Features
- WebSocket endpoint for task updates
- Direct broadcasting of task changes to clients via WebSocket

## 🧠 Redis Usage
- Cache recent boards/tasks
- Add TTL and invalidation logic

## 📨 RabbitMQ Usage
- Queue: "notifications"
- Producer: sends task assignment events
- Consumer: listens and triggers alerts

## 🧰 Development Steps

### Step 1: Project Setup
- Create Spring Boot project
- Add dependencies in pom.xml
- Configure MySQL connection

### Step 2: User Authentication
- Create User entity, repository, service
- Implement JWT login/register
- Secure endpoints with Spring Security

### Step 3: Board & Task Management
- Create Board and Task entities
- Implement CRUD operations
- Add role-based access control

### Step 4: Real-Time Updates
- Configure WebSocket with STOMP (optional)
- Broadcast task changes to clients

### Step 5: Notifications
- Set up RabbitMQ
- Send task assignment events
- Consume and log/display notifications

### Step 6: Caching
- Use Redis to cache recent boards/tasks
- Add TTL and invalidation logic

### Step 7: Documentation & Deployment
- Add Swagger for API docs
- Create Dockerfile and docker-compose.yml
- Test and deploy locally

## 🧪 Testing
- Unit tests for services
- Integration tests for WebSocket and RabbitMQ
- Manual testing via Swagger UI

## 🧾 Notes
- Use UUIDs for entity IDs
- Use BCrypt for password hashing
- Use @Transactional for service methods
- Use @Scheduled for cleanup tasks