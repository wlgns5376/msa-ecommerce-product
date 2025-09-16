import { InventorySKUService } from './InventorySKUService';
import { InventorySKURepository } from '../../infrastructure/repositories/InventorySKURepository';
import { CreateInventorySKUDto } from '../dto/CreateInventorySKUDto';
import { InventorySKU } from '../../domain/inventory/InventorySKU';

jest.mock('../../infrastructure/repositories/InventorySKURepository');

describe('InventorySKUService', () => {
  let service: InventorySKUService;
  let mockRepository: jest.Mocked<InventorySKURepository>;

  beforeEach(() => {
    mockRepository = new InventorySKURepository() as jest.Mocked<InventorySKURepository>;
    service = new InventorySKUService(mockRepository);
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  describe('createSKU', () => {
    it('유효한 데이터로 SKU를 생성해야 한다', async () => {
      const createDto: CreateInventorySKUDto = {
        skuCode: 'SKU-001',
        productId: 'PROD-001',
        warehouseId: 'WH-001',
        quantity: 100,
        reservedQuantity: 10,
        minStockLevel: 20,
        maxStockLevel: 500,
      };

      const savedSKU = new InventorySKU({
        id: 'generated-id',
        skuCode: createDto.skuCode,
        productId: createDto.productId,
        warehouseId: createDto.warehouseId,
        quantity: createDto.quantity,
        reservedQuantity: createDto.reservedQuantity!,
        minStockLevel: createDto.minStockLevel,
        maxStockLevel: createDto.maxStockLevel,
      });

      mockRepository.findBySkuCodeAndWarehouse.mockResolvedValue(null);
      mockRepository.save.mockResolvedValue(savedSKU);

      const result = await service.createSKU(createDto);

      expect(mockRepository.findBySkuCodeAndWarehouse).toHaveBeenCalledWith('SKU-001', 'WH-001');
      expect(mockRepository.save).toHaveBeenCalledWith(expect.any(InventorySKU));
      expect(result).toEqual(savedSKU.toJSON());
    });

    it('예약 수량 없이 SKU를 생성해야 한다', async () => {
      const createDto: CreateInventorySKUDto = {
        skuCode: 'SKU-001',
        productId: 'PROD-001',
        warehouseId: 'WH-001',
        quantity: 100,
        minStockLevel: 20,
        maxStockLevel: 500,
      };

      const savedSKU = new InventorySKU({
        id: 'generated-id',
        ...createDto,
        reservedQuantity: 0,
      });

      mockRepository.findBySkuCodeAndWarehouse.mockResolvedValue(null);
      mockRepository.save.mockResolvedValue(savedSKU);

      const result = await service.createSKU(createDto);

      expect(mockRepository.save).toHaveBeenCalledWith(expect.objectContaining({
        _reservedQuantity: 0,
      }));
      expect(result.reservedQuantity).toBe(0);
    });

    it('중복된 SKU 코드로 생성 시 에러를 발생시켜야 한다', async () => {
      const createDto: CreateInventorySKUDto = {
        skuCode: 'SKU-001',
        productId: 'PROD-001',
        warehouseId: 'WH-001',
        quantity: 100,
        reservedQuantity: 0,
        minStockLevel: 20,
        maxStockLevel: 500,
      };

      const existingSKU = new InventorySKU({
        id: 'existing-id',
        skuCode: createDto.skuCode,
        productId: createDto.productId,
        warehouseId: createDto.warehouseId,
        quantity: createDto.quantity,
        reservedQuantity: createDto.reservedQuantity!,
        minStockLevel: createDto.minStockLevel,
        maxStockLevel: createDto.maxStockLevel,
      });

      mockRepository.findBySkuCodeAndWarehouse.mockResolvedValue(existingSKU);

      await expect(service.createSKU(createDto)).rejects.toThrow('SKU code already exists');

      const error = await service.createSKU(createDto).catch(e => e);
      expect(error.code).toBe('DUPLICATE_SKU');

      expect(mockRepository.save).not.toHaveBeenCalled();
    });

    it('예약 수량이 전체 수량을 초과하면 에러를 발생시켜야 한다', async () => {
      const createDto: CreateInventorySKUDto = {
        skuCode: 'SKU-001',
        productId: 'PROD-001',
        warehouseId: 'WH-001',
        quantity: 50,
        reservedQuantity: 60,
        minStockLevel: 20,
        maxStockLevel: 500,
      };

      mockRepository.findBySkuCodeAndWarehouse.mockResolvedValue(null);

      await expect(service.createSKU(createDto)).rejects.toThrow('Reserved quantity cannot exceed total quantity');

      const error = await service.createSKU(createDto).catch(e => e);
      expect(error.code).toBe('VALIDATION_ERROR');

      expect(mockRepository.save).not.toHaveBeenCalled();
    });

    it('최소 재고 수준이 최대 재고 수준을 초과하면 에러를 발생시켜야 한다', async () => {
      const createDto: CreateInventorySKUDto = {
        skuCode: 'SKU-001',
        productId: 'PROD-001',
        warehouseId: 'WH-001',
        quantity: 100,
        reservedQuantity: 0,
        minStockLevel: 600,
        maxStockLevel: 500,
      };

      mockRepository.findBySkuCodeAndWarehouse.mockResolvedValue(null);

      await expect(service.createSKU(createDto)).rejects.toThrow('Min stock level cannot exceed max stock level');

      const error = await service.createSKU(createDto).catch(e => e);
      expect(error.code).toBe('VALIDATION_ERROR');

      expect(mockRepository.save).not.toHaveBeenCalled();
    });

    it('음수 수량으로 생성 시 에러를 발생시켜야 한다', async () => {
      const createDto: CreateInventorySKUDto = {
        skuCode: 'SKU-001',
        productId: 'PROD-001',
        warehouseId: 'WH-001',
        quantity: -10,
        reservedQuantity: 0,
        minStockLevel: 20,
        maxStockLevel: 500,
      };

      mockRepository.findBySkuCodeAndWarehouse.mockResolvedValue(null);

      await expect(service.createSKU(createDto)).rejects.toThrow('Quantity cannot be negative');

      const error = await service.createSKU(createDto).catch(e => e);
      expect(error.code).toBe('VALIDATION_ERROR');

      expect(mockRepository.save).not.toHaveBeenCalled();
    });

    it('레포지토리 저장 실패 시 에러를 전파해야 한다', async () => {
      const createDto: CreateInventorySKUDto = {
        skuCode: 'SKU-001',
        productId: 'PROD-001',
        warehouseId: 'WH-001',
        quantity: 100,
        reservedQuantity: 0,
        minStockLevel: 20,
        maxStockLevel: 500,
      };

      mockRepository.findBySkuCodeAndWarehouse.mockResolvedValue(null);
      mockRepository.save.mockRejectedValue(new Error('Database error'));

      await expect(service.createSKU(createDto)).rejects.toThrow('Database error');

      expect(mockRepository.findBySkuCodeAndWarehouse).toHaveBeenCalledWith('SKU-001', 'WH-001');
      expect(mockRepository.save).toHaveBeenCalled();
    });

    it('빈 SKU 코드로 생성 시 에러를 발생시켜야 한다', async () => {
      const createDto: CreateInventorySKUDto = {
        skuCode: '',
        productId: 'PROD-001',
        warehouseId: 'WH-001',
        quantity: 100,
        reservedQuantity: 0,
        minStockLevel: 20,
        maxStockLevel: 500,
      };

      mockRepository.findBySkuCodeAndWarehouse.mockResolvedValue(null);

      await expect(service.createSKU(createDto)).rejects.toThrow('SKU code is required');

      const error = await service.createSKU(createDto).catch(e => e);
      expect(error.code).toBe('VALIDATION_ERROR');

      expect(mockRepository.save).not.toHaveBeenCalled();
    });

    it('빈 제품 ID로 생성 시 에러를 발생시켜야 한다', async () => {
      const createDto: CreateInventorySKUDto = {
        skuCode: 'SKU-001',
        productId: '',
        warehouseId: 'WH-001',
        quantity: 100,
        reservedQuantity: 0,
        minStockLevel: 20,
        maxStockLevel: 500,
      };

      mockRepository.findBySkuCodeAndWarehouse.mockResolvedValue(null);

      await expect(service.createSKU(createDto)).rejects.toThrow('Product ID is required');

      const error = await service.createSKU(createDto).catch(e => e);
      expect(error.code).toBe('VALIDATION_ERROR');

      expect(mockRepository.save).not.toHaveBeenCalled();
    });

    it('빈 창고 ID로 생성 시 에러를 발생시켜야 한다', async () => {
      const createDto: CreateInventorySKUDto = {
        skuCode: 'SKU-001',
        productId: 'PROD-001',
        warehouseId: '',
        quantity: 100,
        reservedQuantity: 0,
        minStockLevel: 20,
        maxStockLevel: 500,
      };

      mockRepository.findBySkuCodeAndWarehouse.mockResolvedValue(null);

      await expect(service.createSKU(createDto)).rejects.toThrow('Warehouse ID is required');

      const error = await service.createSKU(createDto).catch(e => e);
      expect(error.code).toBe('VALIDATION_ERROR');

      expect(mockRepository.save).not.toHaveBeenCalled();
    });
  });
});