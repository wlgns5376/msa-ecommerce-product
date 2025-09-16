import express, { Application, Request, Response } from 'express';
import { InventoryController } from './interfaces/api/controllers/InventoryController';
import { InventorySKUService } from './application/services/InventorySKUService';
import { InventorySKURepository } from './infrastructure/repositories/InventorySKURepository';

export function createApp(): Application {
  const app = express();

  app.use(express.json());
  app.use(express.urlencoded({ extended: true }));

  const inventorySKURepository = new InventorySKURepository();
  const inventorySKUService = new InventorySKUService(inventorySKURepository);
  const inventoryController = new InventoryController(inventorySKUService);

  app.get('/health', (_req: Request, res: Response) => {
    res.json({ status: 'ok', timestamp: new Date().toISOString() });
  });

  app.use('/api/inventory', inventoryController.getRouter());

  app.use((req: Request, res: Response) => {
    res.status(404).json({
      success: false,
      error: {
        message: 'Resource not found',
        path: req.path,
      },
    });
  });

  app.use((err: Error, _req: Request, res: Response) => {
    console.error('Unhandled error:', err);
    res.status(500).json({
      success: false,
      error: {
        message: 'Internal server error',
      },
    });
  });

  return app;
}