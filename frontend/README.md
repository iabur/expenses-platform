# 💰 Expenses Platform Frontend

A modern, production-grade React TypeScript frontend for the next-generation expense sharing platform.

## 🚀 Features

### ✅ Implemented Features
- **Modern Tech Stack**: React 19, TypeScript, Vite, Tailwind CSS
- **Authentication**: JWT-based auth with Keycloak integration
- **API Integration**: Comprehensive API client with React Query
- **State Management**: Zustand for global state, React Query for server state
- **UI Components**: Radix UI primitives with custom design system
- **Form Handling**: React Hook Form with Zod validation
- **Routing**: React Router with protected routes
- **Responsive Design**: Mobile-first approach with Tailwind CSS
- **Type Safety**: Full TypeScript coverage with strict mode

### 🎨 UI/UX Features
- **Design System**: Consistent component library with dark/light mode support
- **Modern Layout**: Sidebar navigation with responsive design
- **Interactive Forms**: Dynamic expense creation with participant management
- **Real-time Updates**: Live data synchronization with React Query
- **Loading States**: Skeleton loaders and loading indicators
- **Error Handling**: Comprehensive error boundaries and user feedback

### 📱 Pages & Components
- **Dashboard**: Overview with statistics and recent activities
- **Groups**: Group management and creation
- **Expenses**: Expense tracking and creation
- **Profile**: User profile management
- **Settings**: Application preferences

## 🛠️ Tech Stack

### Core Technologies
- **React 19** - Latest React with concurrent features
- **TypeScript** - Type-safe development
- **Vite** - Fast build tool and dev server
- **Tailwind CSS** - Utility-first CSS framework

### State Management
- **Zustand** - Lightweight state management
- **React Query** - Server state management and caching
- **React Hook Form** - Form state and validation

### UI Components
- **Radix UI** - Accessible component primitives
- **Lucide React** - Beautiful icons
- **Class Variance Authority** - Component variants

### Development Tools
- **ESLint** - Code linting
- **Prettier** - Code formatting
- **Vitest** - Testing framework
- **TypeScript** - Static type checking

## 🚀 Getting Started

### Prerequisites
- Node.js 18+ 
- npm or yarn
- Backend services running on localhost:8080 (optional for development mode)

### Installation

1. **Install dependencies**
   ```bash
   npm install
   ```

2. **Set up environment variables**
   ```bash
   cp .env.example .env.local
   ```
   
   Configure your environment variables:
   ```env
   VITE_API_BASE_URL=http://localhost:8080
   VITE_KEYCLOAK_URL=http://localhost:8090
   VITE_KEYCLOAK_REALM=expenses
   VITE_KEYCLOAK_CLIENT_ID=web
   ```

3. **Start development server**
   ```bash
   npm run dev
   ```

4. **Open in browser**
   Navigate to `http://localhost:3000`

### 🧪 Development Mode

The frontend includes a **Development Mode** that uses mock data, allowing you to explore all features without needing the backend services running.

**Features in Development Mode:**
- ✅ **Mock Authentication** - Any email/password combination works
- ✅ **Sample Data** - Pre-populated groups, expenses, and users
- ✅ **Full UI** - All components and interactions work
- ✅ **No Backend Required** - Completely self-contained

**Demo Credentials:**
- `alice@example.com` / `password`
- `bob@example.com` / `password`
- Or any email/password combination

### 🔧 Configuration

**Development Configuration** (`src/config/development.ts`):
```typescript
export const DEV_CONFIG = {
  USE_MOCK_DATA: true,  // Set to false to use real API
  MOCK_DELAY: 500,      // Simulate network delay
  // ... demo credentials
};
```

**Switch to Real API:**
1. Set `USE_MOCK_DATA: false` in `src/config/development.ts`
2. Ensure backend services are running on `localhost:8080`
3. Restart the development server

### Available Scripts

```bash
# Development
npm run dev              # Start development server
npm run build            # Build for production
npm run preview          # Preview production build

# Code Quality
npm run lint             # Run ESLint
npm run lint:fix         # Fix ESLint issues
npm run format           # Format code with Prettier
npm run type-check       # Run TypeScript checks

# Testing
npm run test             # Run tests
npm run test:ui          # Run tests with UI
npm run test:coverage    # Run tests with coverage

# Utilities
npm run clean            # Clean build artifacts
```

## 🏗️ Project Structure

```
src/
├── components/           # Reusable UI components
│   ├── ui/              # Base UI components (Button, Card, etc.)
│   ├── layout/          # Layout components (Header, Sidebar)
│   ├── forms/           # Form components
│   └── charts/          # Chart components
├── hooks/               # Custom React hooks
├── lib/                 # Utility functions
├── pages/               # Page components
├── services/            # API services
├── stores/              # Zustand stores
├── types/               # TypeScript type definitions
└── utils/               # Helper utilities
```

## 🔧 Configuration

### Environment Variables
- `VITE_API_BASE_URL` - Backend API base URL
- `VITE_KEYCLOAK_URL` - Keycloak server URL
- `VITE_KEYCLOAK_REALM` - Keycloak realm name
- `VITE_KEYCLOAK_CLIENT_ID` - Keycloak client ID

### Tailwind Configuration
The project uses a custom Tailwind configuration with:
- CSS variables for theming
- Custom color palette
- Responsive breakpoints
- Animation utilities

## 🎨 Design System

### Color Palette
- **Primary**: Blue tones for main actions
- **Secondary**: Gray tones for secondary elements
- **Success**: Green for positive actions
- **Warning**: Yellow for warnings
- **Destructive**: Red for errors/destructive actions

### Typography
- **Font Family**: System font stack
- **Font Sizes**: Consistent scale from 12px to 48px
- **Font Weights**: 400 (normal), 500 (medium), 600 (semibold), 700 (bold)

### Spacing
- **Base Unit**: 4px (0.25rem)
- **Scale**: 0, 1, 2, 3, 4, 5, 6, 8, 10, 12, 16, 20, 24, 32, 40, 48, 56, 64

## 🔒 Authentication

The frontend integrates with Keycloak for authentication:
- JWT token management
- Automatic token refresh
- Protected routes
- User profile management

## 📊 State Management

### Global State (Zustand)
- Authentication state
- User preferences
- UI state (modals, themes)

### Server State (React Query)
- API data caching
- Background updates
- Optimistic updates
- Error handling

## 🧪 Testing

The project includes comprehensive testing setup:
- **Unit Tests**: Component testing with Vitest
- **Integration Tests**: API integration testing
- **E2E Tests**: Full user journey testing (planned)

## 🚀 Deployment

### Production Build
```bash
npm run build
```

### Preview Build
```bash
npm run build:preview
npm run preview
```

### Docker Deployment
```dockerfile
FROM node:18-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Run linting and formatting
6. Submit a pull request

## 📄 License

This project is part of the Expenses Platform and follows the same licensing terms.

## 🔗 Related Projects

- [Backend Services](../services/) - Microservices backend
- [API Documentation](../docs/API_GUIDE.md) - API reference
- [Architecture Guide](../docs/ARCHITECTURE.md) - System architecture

## 🔧 Troubleshooting

### Common Issues

**1. Login Not Working**
- ✅ **Solution**: The app uses mock data by default - any email/password will work
- Look for the "Development Mode" banner on the login page
- Check `src/config/development.ts` to ensure `USE_MOCK_DATA: true`

**2. API Connection Errors**
- ✅ **Solution**: In development mode, no backend is required
- All API calls are mocked with sample data
- To use real API, set `USE_MOCK_DATA: false` and ensure backend is running

**3. Build Errors**
```bash
# Clear cache and reinstall
npm run clean
rm -rf node_modules
npm install
npm run build
```

**4. TypeScript Errors**
```bash
# Run type checking
npm run type-check

# Fix linting issues
npm run lint:fix
```

**5. Styling Issues**
- Ensure Tailwind CSS is properly configured
- Check `tailwind.config.js` and `postcss.config.js`
- Restart the development server after config changes

### 🐛 Debug Mode

Enable debug logging by adding to your browser console:
```javascript
localStorage.setItem('debug', 'true');
```

### 📱 Mobile Testing

The app is responsive and works on mobile devices:
- Test on different screen sizes using browser dev tools
- Use `npm run preview` to test production build locally

### 🔄 Switching Between Modes

**Development Mode (Mock Data):**
- ✅ No backend required
- ✅ Instant responses
- ✅ Sample data included
- ✅ Perfect for UI/UX testing

**Production Mode (Real API):**
- ✅ Real backend integration
- ✅ Actual data persistence
- ✅ Full authentication flow
- ✅ Production-ready

---

Built with ❤️ using modern web technologies