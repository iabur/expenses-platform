// Development configuration
export const DEV_CONFIG = {
  // Set to true to use mock data instead of real API calls
  USE_MOCK_DATA: true,
  
  // Mock data settings
  MOCK_DELAY: 500, // Simulate network delay in milliseconds
  
  // Demo user credentials
  DEMO_CREDENTIALS: {
    alice: {
      email: 'alice@example.com',
      password: 'password',
      name: 'Alice Johnson'
    },
    bob: {
      email: 'bob@example.com', 
      password: 'password',
      name: 'Bob Smith'
    }
  }
};

// Helper function to simulate API delay
export const simulateDelay = (ms: number = DEV_CONFIG.MOCK_DELAY) => 
  new Promise(resolve => setTimeout(resolve, ms));
