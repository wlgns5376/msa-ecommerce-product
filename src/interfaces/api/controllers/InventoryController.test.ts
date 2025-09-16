import request from 'supertest';
import express, { Express } from 'express';
import { InventoryController } from './InventoryController';
import { InventorySKUService } from '../../../application/services/InventorySKUService';
import { CreateInventorySKUDto } from '../../../application/dto/CreateInventorySKUDto';

jest.mock('../../../application/services/InventorySKUService');

describe('InventoryController', () => {
  let app: Express;
  let inventoryController: InventoryController;
  let mockService: jest.Mocked<InventorySKUService>;

  beforeEach(() => {
    app = express();
    app.use(express.json());

    mockService = new InventorySKUService({} as never) as jest.Mocked<InventorySKUService>;
    inventoryController = new InventoryController(mockService);

    app.use('/api/inventory', inventoryController.getRouter());
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  describe('POST /api/inventory/skus', () => {
    it('유효한 데이터로 SKU를 생성해야 한다', async () => {
      const createDto: CreateInventorySKUDto = {
        skuCode: 'SKU-001',
        productId: 'PROD-001',
        warehouseId: 'WH-001',
        quantity: 100,
        reservedQuantity: 0,
        minStockLevel: 20,
        maxStockLevel: 500,
      };

      const expectedResult = {
        id: 'generated-uuid',
        ...createDto,
        availableQuantity: 100,
        isLowStock: false,
        isOverStock: false,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      };

      mockService.createSKU.mockResolvedValue(expectedResult);

      const response = await request(app)
        .post('/api/inventory/skus')
        .send(createDto)
        .expect(201);

      expect(response.body).toEqual({
        success: true,
        data: expectedResult,
      });

      expect(mockService.createSKU).toHaveBeenCalledWith(createDto);
    });

    it('필수 필드가 누락되면 400 에러를 반환해야 한다', async () => {
      const invalidDto = {
        skuCode: 'SKU-001',
        productId: 'PROD-001',
      };

      const response = await request(app)
        .post('/api/inventory/skus')
        .send(invalidDto)
        .expect(400);

      expect(response.body).toEqual({
        success: false,
        error: expect.objectContaining({
          message: expect.stringContaining('Validation failed'),
          errors: expect.arrayContaining([
            expect.objectContaining({
              field: 'warehouseId',
              message: 'Warehouse ID is required',
            }),
            expect.objectContaining({
              field: 'quantity',
              message: 'Quantity is required',
            }),
            expect.objectContaining({
              field: 'minStockLevel',
              message: 'Min stock level is required',
            }),
            expect.objectContaining({
              field: 'maxStockLevel',
              message: 'Max stock level is required',
            }),
          ]),
        }),
      });

      expect(mockService.createSKU).not.toHaveBeenCalled();
    });

    it('음수 수량으로 요청하면 400 에러를 반환해야 한다', async () => {
      const invalidDto = {
        skuCode: 'SKU-001',
        productId: 'PROD-001',
        warehouseId: 'WH-001',
        quantity: -10,
        reservedQuantity: 0,
        minStockLevel: 20,
        maxStockLevel: 500,
      };

      const response = await request(app)
        .post('/api/inventory/skus')
        .send(invalidDto)
        .expect(400);

      expect(response.body).toEqual({
        success: false,
        error: expect.objectContaining({
          message: expect.stringContaining('Validation failed'),
          errors: expect.arrayContaining([
            expect.objectContaining({
              field: 'quantity',
              message: 'Quantity must be non-negative',
            }),
          ]),
        }),
      });

      expect(mockService.createSKU).not.toHaveBeenCalled();
    });

    it('음수 예약 수량으로 요청하면 400 에러를 반환해야 한다', async () => {
      const invalidDto = {
        skuCode: 'SKU-001',
        productId: 'PROD-001',
        warehouseId: 'WH-001',
        quantity: 100,
        reservedQuantity: -10,
        minStockLevel: 20,
        maxStockLevel: 500,
      };

      const response = await request(app)
        .post('/api/inventory/skus')
        .send(invalidDto)
        .expect(400);

      expect(response.body).toEqual({
        success: false,
        error: expect.objectContaining({
          message: expect.stringContaining('Validation failed'),
          errors: expect.arrayContaining([
            expect.objectContaining({
              field: 'reservedQuantity',
              message: 'Reserved quantity must be non-negative',
            }),
          ]),
        }),
      });

      expect(mockService.createSKU).not.toHaveBeenCalled();
    });

    it('빈 문자열 SKU 코드로 요청하면 400 에러를 반환해야 한다', async () => {
      const invalidDto = {
        skuCode: '',
        productId: 'PROD-001',
        warehouseId: 'WH-001',
        quantity: 100,
        reservedQuantity: 0,
        minStockLevel: 20,
        maxStockLevel: 500,
      };

      const response = await request(app)
        .post('/api/inventory/skus')
        .send(invalidDto)
        .expect(400);

      expect(response.body).toEqual({
        success: false,
        error: expect.objectContaining({
          message: expect.stringContaining('Validation failed'),
          errors: expect.arrayContaining([
            expect.objectContaining({
              field: 'skuCode',
              message: 'SKU code cannot be empty',
            }),
          ]),
        }),
      });

      expect(mockService.createSKU).not.toHaveBeenCalled();
    });

    it('서비스에서 에러가 발생하면 500 에러를 반환해야 한다', async () => {
      const createDto: CreateInventorySKUDto = {
        skuCode: 'SKU-001',
        productId: 'PROD-001',
        warehouseId: 'WH-001',
        quantity: 100,
        reservedQuantity: 0,
        minStockLevel: 20,
        maxStockLevel: 500,
      };

      mockService.createSKU.mockRejectedValue(new Error('Database connection failed'));

      const response = await request(app)
        .post('/api/inventory/skus')
        .send(createDto)
        .expect(500);

      expect(response.body).toEqual({
        success: false,
        error: {
          message: 'Internal server error',
        },
      });

      expect(mockService.createSKU).toHaveBeenCalledWith(createDto);
    });

    it('중복된 SKU 코드로 요청하면 409 에러를 반환해야 한다', async () => {
      const createDto: CreateInventorySKUDto = {
        skuCode: 'SKU-001',
        productId: 'PROD-001',
        warehouseId: 'WH-001',
        quantity: 100,
        reservedQuantity: 0,
        minStockLevel: 20,
        maxStockLevel: 500,
      };

      const duplicateError = new Error('SKU code already exists') as Error & { code?: string };
      duplicateError.code = 'DUPLICATE_SKU';

      mockService.createSKU.mockRejectedValue(duplicateError);

      const response = await request(app)
        .post('/api/inventory/skus')
        .send(createDto)
        .expect(409);

      expect(response.body).toEqual({
        success: false,
        error: {
          message: 'SKU code already exists',
        },
      });

      expect(mockService.createSKU).toHaveBeenCalledWith(createDto);
    });

    it('예약 수량이 기본값 0으로 설정되어야 한다', async () => {
      const createDto = {
        skuCode: 'SKU-001',
        productId: 'PROD-001',
        warehouseId: 'WH-001',
        quantity: 100,
        minStockLevel: 20,
        maxStockLevel: 500,
      };

      const expectedResult = {
        id: 'generated-uuid',
        ...createDto,
        reservedQuantity: 0,
        availableQuantity: 100,
        isLowStock: false,
        isOverStock: false,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      };

      mockService.createSKU.mockResolvedValue(expectedResult);

      const response = await request(app)
        .post('/api/inventory/skus')
        .send(createDto)
        .expect(201);

      expect(response.body).toEqual({
        success: true,
        data: expectedResult,
      });

      expect(mockService.createSKU).toHaveBeenCalledWith({
        ...createDto,
        reservedQuantity: 0,
      });
    });

    it('잘못된 데이터 타입으로 요청하면 400 에러를 반환해야 한다', async () => {
      const invalidDto = {
        skuCode: 'SKU-001',
        productId: 'PROD-001',
        warehouseId: 'WH-001',
        quantity: 'not-a-number',
        reservedQuantity: 0,
        minStockLevel: 20,
        maxStockLevel: 500,
      };

      const response = await request(app)
        .post('/api/inventory/skus')
        .send(invalidDto)
        .expect(400);

      expect(response.body).toEqual({
        success: false,
        error: expect.objectContaining({
          message: expect.stringContaining('Validation failed'),
          errors: expect.arrayContaining([
            expect.objectContaining({
              field: 'quantity',
              message: 'Quantity must be a number',
            }),
          ]),
        }),
      });

      expect(mockService.createSKU).not.toHaveBeenCalled();
    });
  });
});