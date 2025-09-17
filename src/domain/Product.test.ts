import { validateUpdateProductDto, ProductValidationError, UpdateProductDto } from './Product';

describe('Product Domain', () => {
  describe('validateUpdateProductDto', () => {
    describe('Given valid update data', () => {
      it('Then should not throw any error', () => {
        const validDto: UpdateProductDto = {
          name: 'Valid Name',
          description: 'Valid Description',
          price: 100,
          stock: 10,
          categoryId: 'cat-1',
        };

        expect(() => validateUpdateProductDto(validDto)).not.toThrow();
      });

      it('Then should accept empty object', () => {
        expect(() => validateUpdateProductDto({})).not.toThrow();
      });

      it('Then should accept undefined fields', () => {
        const dto: UpdateProductDto = {
          name: undefined,
          price: undefined,
        };

        expect(() => validateUpdateProductDto(dto)).not.toThrow();
      });
    });

    describe('Given invalid name', () => {
      it('Then should throw error for empty string', () => {
        const dto: UpdateProductDto = { name: '' };
        
        expect(() => validateUpdateProductDto(dto))
          .toThrow(new ProductValidationError('Product name cannot be empty'));
      });

      it('Then should throw error for whitespace only', () => {
        const dto: UpdateProductDto = { name: '   ' };
        
        expect(() => validateUpdateProductDto(dto))
          .toThrow(new ProductValidationError('Product name cannot be empty'));
      });
    });

    describe('Given invalid price', () => {
      it('Then should throw error for negative price', () => {
        const dto: UpdateProductDto = { price: -1 };
        
        expect(() => validateUpdateProductDto(dto))
          .toThrow(new ProductValidationError('Product price cannot be negative'));
      });

      it('Then should throw error for NaN', () => {
        const dto: UpdateProductDto = { price: NaN };
        
        expect(() => validateUpdateProductDto(dto))
          .toThrow(new ProductValidationError('Product price must be a valid number'));
      });

      it('Then should throw error for Infinity', () => {
        const dto: UpdateProductDto = { price: Infinity };
        
        expect(() => validateUpdateProductDto(dto))
          .toThrow(new ProductValidationError('Product price must be a valid number'));
      });
    });

    describe('Given invalid stock', () => {
      it('Then should throw error for negative stock', () => {
        const dto: UpdateProductDto = { stock: -1 };
        
        expect(() => validateUpdateProductDto(dto))
          .toThrow(new ProductValidationError('Product stock cannot be negative'));
      });

      it('Then should throw error for non-integer stock', () => {
        const dto: UpdateProductDto = { stock: 1.5 };
        
        expect(() => validateUpdateProductDto(dto))
          .toThrow(new ProductValidationError('Product stock must be an integer'));
      });
    });

    describe('Given invalid categoryId', () => {
      it('Then should throw error for empty string', () => {
        const dto: UpdateProductDto = { categoryId: '' };
        
        expect(() => validateUpdateProductDto(dto))
          .toThrow(new ProductValidationError('Category ID cannot be empty'));
      });

      it('Then should throw error for whitespace only', () => {
        const dto: UpdateProductDto = { categoryId: '  ' };
        
        expect(() => validateUpdateProductDto(dto))
          .toThrow(new ProductValidationError('Category ID cannot be empty'));
      });
    });
  });

  describe('ProductValidationError', () => {
    it('should have correct name', () => {
      const error = new ProductValidationError('Test message');
      expect(error.name).toBe('ProductValidationError');
      expect(error.message).toBe('Test message');
    });
  });
});