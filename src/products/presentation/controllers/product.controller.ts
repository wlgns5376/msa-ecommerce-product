import {
  Controller,
  Get,
  Post,
  Body,
  Patch,
  Param,
  Delete,
  HttpCode,
  HttpStatus,
  ValidationPipe,
  Query,
} from '@nestjs/common';
import { ProductService } from '../../application/product.service';
import { CreateProductDto } from '../dto/create-product.dto';
import { UpdateProductDto } from '../dto/update-product.dto';
import { Product } from '../../domain/product.entity';

@Controller('products')
export class ProductController {
  constructor(private readonly productService: ProductService) {}

  @Post()
  @HttpCode(HttpStatus.CREATED)
  async create(
    @Body(new ValidationPipe({ whitelist: true, forbidNonWhitelisted: true }))
    createProductDto: CreateProductDto,
  ): Promise<Product> {
    return await this.productService.create(createProductDto);
  }

  @Get()
  async findAll(@Query('category') category?: string): Promise<Product[]> {
    if (category) {
      return await this.productService.findByCategory(category);
    }
    return await this.productService.findAll();
  }

  @Get(':id')
  async findOne(@Param('id') id: string): Promise<Product> {
    return await this.productService.findOne(id);
  }

  @Get('sku/:sku')
  async findBySku(@Param('sku') sku: string): Promise<Product> {
    return await this.productService.findBySku(sku);
  }

  @Patch(':id')
  async update(
    @Param('id') id: string,
    @Body(new ValidationPipe({ whitelist: true, forbidNonWhitelisted: true }))
    updateProductDto: UpdateProductDto,
  ): Promise<Product> {
    return await this.productService.update(id, updateProductDto);
  }

  @Delete(':id')
  @HttpCode(HttpStatus.NO_CONTENT)
  async remove(@Param('id') id: string): Promise<void> {
    return await this.productService.remove(id);
  }
}