import { Request, Response, NextFunction } from 'express';
import { body, validationResult, ValidationChain } from 'express-validator';

export const validateCreateInventorySKU = (): ValidationChain[] => {
  return [
    body('skuCode')
      .notEmpty()
      .withMessage('SKU code is required')
      .isString()
      .withMessage('SKU code must be a string')
      .trim()
      .isLength({ min: 1 })
      .withMessage('SKU code cannot be empty'),
    body('productId')
      .notEmpty()
      .withMessage('Product ID is required')
      .isString()
      .withMessage('Product ID must be a string')
      .trim()
      .isLength({ min: 1 })
      .withMessage('Product ID cannot be empty'),
    body('warehouseId')
      .notEmpty()
      .withMessage('Warehouse ID is required')
      .isString()
      .withMessage('Warehouse ID must be a string')
      .trim()
      .isLength({ min: 1 })
      .withMessage('Warehouse ID cannot be empty'),
    body('quantity')
      .notEmpty()
      .withMessage('Quantity is required')
      .isNumeric()
      .withMessage('Quantity must be a number')
      .isInt({ min: 0 })
      .withMessage('Quantity must be non-negative'),
    body('reservedQuantity')
      .optional()
      .isNumeric()
      .withMessage('Reserved quantity must be a number')
      .isInt({ min: 0 })
      .withMessage('Reserved quantity must be non-negative'),
    body('minStockLevel')
      .notEmpty()
      .withMessage('Min stock level is required')
      .isNumeric()
      .withMessage('Min stock level must be a number')
      .isInt({ min: 0 })
      .withMessage('Min stock level must be non-negative'),
    body('maxStockLevel')
      .notEmpty()
      .withMessage('Max stock level is required')
      .isNumeric()
      .withMessage('Max stock level must be a number')
      .isInt({ min: 0 })
      .withMessage('Max stock level must be non-negative'),
  ];
};

export const handleValidationErrors = (
  req: Request,
  res: Response,
  next: NextFunction
): void => {
  const errors = validationResult(req);

  if (!errors.isEmpty()) {
    const validationErrors = errors.array().map((error) => ({
      field: error.type === 'field' ? error.path : 'unknown',
      message: error.msg,
    }));

    res.status(400).json({
      success: false,
      error: {
        message: 'Validation failed',
        errors: validationErrors,
      },
    });
    return;
  }

  next();
};