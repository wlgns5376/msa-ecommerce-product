import { Request, Response, Router } from 'express';
import { InventorySKUService } from '../../../application/services/InventorySKUService';
import { CreateInventorySKUDto } from '../../../application/dto/CreateInventorySKUDto';
import { validateCreateInventorySKU, handleValidationErrors } from '../middleware/validationMiddleware';

interface ErrorWithCode extends Error {
  code?: string;
}

export class InventoryController {
  private router: Router;
  private inventorySKUService: InventorySKUService;

  constructor(inventorySKUService: InventorySKUService) {
    this.router = Router();
    this.inventorySKUService = inventorySKUService;
    this.setupRoutes();
  }

  private setupRoutes(): void {
    this.router.post(
      '/skus',
      validateCreateInventorySKU(),
      handleValidationErrors,
      this.createSKU.bind(this)
    );
  }

  private async createSKU(req: Request, res: Response): Promise<void> {
    try {
      const createDto: CreateInventorySKUDto = {
        skuCode: req.body.skuCode,
        productId: req.body.productId,
        warehouseId: req.body.warehouseId,
        quantity: Number(req.body.quantity),
        reservedQuantity: req.body.reservedQuantity !== undefined 
          ? Number(req.body.reservedQuantity) 
          : 0,
        minStockLevel: Number(req.body.minStockLevel),
        maxStockLevel: Number(req.body.maxStockLevel),
      };

      const result = await this.inventorySKUService.createSKU(createDto);

      res.status(201).json({
        success: true,
        data: result,
      });
    } catch (error) {
      this.handleError(error, res);
    }
  }

  private handleError(error: unknown, res: Response): void {
    if (error instanceof Error) {
      const errorWithCode = error as ErrorWithCode;
      
      if (errorWithCode.code === 'DUPLICATE_SKU') {
        res.status(409).json({
          success: false,
          error: {
            message: error.message,
          },
        });
        return;
      }

      if (errorWithCode.code === 'VALIDATION_ERROR') {
        res.status(400).json({
          success: false,
          error: {
            message: error.message,
          },
        });
        return;
      }
    }

    console.error('Unexpected error:', error);
    res.status(500).json({
      success: false,
      error: {
        message: 'Internal server error',
      },
    });
  }

  public getRouter(): Router {
    return this.router;
  }
}