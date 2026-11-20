package com.example.sarisari.manager.domain.model

/** Thrown at checkout when a product in the cart no longer exists or has been archived. */
class ProductUnavailableException(val productId: Long) : Exception("Product $productId is not available")

class SaleNotFoundException(val saleId: Long) : Exception("Sale $saleId does not exist")

class SaleAlreadyVoidedException(val saleId: Long) : Exception("Sale $saleId is already voided")
