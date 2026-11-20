package com.example.sarisari.manager.domain.model

/** Thrown when a stock decrease would take a product's stock below zero. */
class InsufficientStockException(val available: Int, val requested: Int) :
    Exception("Only $available in stock, cannot remove $requested")
