package stos.dynamictestspec.cart;

import java.util.List;

public record CartSnapshot(CartStatus status, List<Product> items) {
}
