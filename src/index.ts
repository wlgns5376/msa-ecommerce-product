import { createApp } from './app';

const PORT = process.env.PORT || 3000;

const app = createApp();

app.listen(PORT, () => {
  console.log(`Server is running on port ${PORT}`);
  console.log(`Health check: http://localhost:${PORT}/health`);
  console.log(`API endpoint: http://localhost:${PORT}/api/inventory/skus`);
});