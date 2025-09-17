# MSA E-commerce Product Service

Product management microservice for e-commerce platform.

## Features

- PUT /api/products/{id} - Update product by ID

## Installation

```bash
npm install
```

## Development

```bash
npm run dev
```

## Testing

```bash
npm test
npm run test:coverage
```

## Build

```bash
npm run build
```

## API Documentation

### Update Product

**PUT** `/api/products/{id}`

Update an existing product by ID.

#### Request Body

```json
{
  "name": "string",
  "description": "string", 
  "price": number,
  "stock": number,
  "categoryId": "string"
}
```

#### Response

- **200 OK** - Product updated successfully
- **400 Bad Request** - Validation error
- **404 Not Found** - Product not found
- **500 Internal Server Error** - Server error

## Code Quality

- Test Coverage: 97%+
- TypeScript strict mode
- ESLint configured
- TDD approach