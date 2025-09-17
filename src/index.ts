import { createApp } from './app';

const PORT = process.env['PORT'] || 3000;

const app = createApp();

app.listen(PORT, () => {
  console.error(`Product service is running on port ${PORT}`);
});