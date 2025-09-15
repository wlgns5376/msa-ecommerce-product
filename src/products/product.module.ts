import { Module } from '@nestjs/common';
import { ProductController } from './presentation/controllers/product.controller';
import { ProductService } from './application/product.service';
import { InMemoryProductRepository } from './infrastructure/persistence/in-memory-product.repository';

@Module({
  controllers: [ProductController],
  providers: [
    ProductService,
    {
      provide: 'IProductRepository',
      useClass: InMemoryProductRepository,
    },
  ],
  exports: [ProductService],
})
export class ProductModule {}