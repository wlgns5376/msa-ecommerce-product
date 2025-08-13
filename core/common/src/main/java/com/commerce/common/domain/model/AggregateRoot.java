package com.commerce.common.domain.model;

public abstract class AggregateRoot<ID> extends BaseEntity {
    public abstract ID getId();
}