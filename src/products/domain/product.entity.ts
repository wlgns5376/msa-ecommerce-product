export class Product {
  readonly id: string;
  readonly name: string;
  readonly description: string;
  readonly price: number;
  readonly stock: number;
  readonly sku: string;
  readonly category: string;
  readonly isActive: boolean;
  readonly createdAt: Date;
  readonly updatedAt: Date;

  constructor(
    id: string,
    name: string,
    description: string,
    price: number,
    stock: number,
    sku: string,
    category: string,
    isActive: boolean = true,
    createdAt: Date = new Date(),
    updatedAt: Date = new Date(),
  ) {
    this.validatePrice(price);
    this.validateStock(stock);
    this.validateName(name);
    this.validateSku(sku);

    this.id = id;
    this.name = name;
    this.description = description;
    this.price = price;
    this.stock = stock;
    this.sku = sku;
    this.category = category;
    this.isActive = isActive;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  private validatePrice(price: number): void {
    if (price <= 0) {
      throw new Error('Price must be greater than 0');
    }
  }

  private validateStock(stock: number): void {
    if (stock < 0) {
      throw new Error('Stock cannot be negative');
    }
  }

  private validateName(name: string): void {
    if (!name || name.trim().length === 0) {
      throw new Error('Product name is required');
    }
  }

  private validateSku(sku: string): void {
    if (!sku || sku.trim().length === 0) {
      throw new Error('SKU is required');
    }
  }

  static create(
    name: string,
    description: string,
    price: number,
    stock: number,
    sku: string,
    category: string,
    isActive: boolean = true,
  ): Product {
    const id = Product.generateId();
    return new Product(
      id,
      name,
      description,
      price,
      stock,
      sku,
      category,
      isActive,
    );
  }

  private static generateId(): string {
    return `${Date.now()}-${Math.random().toString(36).substring(2, 9)}`;
  }

  update(data: Partial<{
    name: string;
    description: string;
    price: number;
    stock: number;
    sku: string;
    category: string;
    isActive: boolean;
  }>): Product {
    const updatedData = {
      name: data.name ?? this.name,
      description: data.description ?? this.description,
      price: data.price ?? this.price,
      stock: data.stock ?? this.stock,
      sku: data.sku ?? this.sku,
      category: data.category ?? this.category,
      isActive: data.isActive ?? this.isActive,
    };

    return new Product(
      this.id,
      updatedData.name,
      updatedData.description,
      updatedData.price,
      updatedData.stock,
      updatedData.sku,
      updatedData.category,
      updatedData.isActive,
      this.createdAt,
      new Date(),
    );
  }

  canBePurchased(quantity: number): boolean {
    return this.isActive && this.stock >= quantity;
  }

  decreaseStock(quantity: number): Product {
    if (quantity <= 0) {
      throw new Error('Quantity must be greater than 0');
    }
    if (this.stock < quantity) {
      throw new Error('Insufficient stock');
    }
    return this.update({ stock: this.stock - quantity });
  }

  increaseStock(quantity: number): Product {
    if (quantity <= 0) {
      throw new Error('Quantity must be greater than 0');
    }
    return this.update({ stock: this.stock + quantity });
  }
}