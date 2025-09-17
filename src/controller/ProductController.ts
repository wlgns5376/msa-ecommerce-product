import { Router, Request, Response } from 'express';
import { ProductService, ProductNotFoundError } from '../service/ProductService';
import { UpdateProductDto, ProductValidationError } from '../domain/Product';

export function createProductRouter(productService: ProductService): Router {
  const router = Router();

  router.put('/:id', async (req: Request, res: Response): Promise<void> => {
    try {
      const productId = req.params['id']!;
      const updateDto: UpdateProductDto = req.body;
      const updatedProduct = await productService.updateProduct(productId, updateDto);
      
      res.status(200).json(updatedProduct);
    } catch (error) {
      if (error instanceof ProductNotFoundError) {
        res.status(404).json({ error: error.message });
      } else if (error instanceof ProductValidationError) {
        res.status(400).json({ error: error.message });
      } else {
        console.error('Unexpected error:', error);
        res.status(500).json({ error: 'Internal server error' });
      }
    }
  });

  return router;
}