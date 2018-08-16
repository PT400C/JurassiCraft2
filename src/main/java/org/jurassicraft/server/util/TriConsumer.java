package org.jurassicraft.server.util;

import java.util.Objects;

@SuppressWarnings("hiding")
@FunctionalInterface
public interface TriConsumer<DinosaurEntity, EntityPlayer, Integer> {
	
  public void accept(DinosaurEntity t, EntityPlayer u, Integer v);

  public default TriConsumer<DinosaurEntity, EntityPlayer, Integer> andThen(TriConsumer<? super DinosaurEntity, ? super EntityPlayer, ? super Integer> after) {
    Objects.requireNonNull(after);
    return (a, b, c) -> {
      accept(a, b, c);
      after.accept(a, b, c);
    };
  }
}