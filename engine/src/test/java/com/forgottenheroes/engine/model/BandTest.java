package com.forgottenheroes.engine.model;

import static com.forgottenheroes.engine.testing.TestFixtures.band;
import static com.forgottenheroes.engine.testing.TestFixtures.unit;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class BandTest {

    @Test
    void indexZeroIsTheFrontAndTheLastIsTheBack() {
        Unit a = unit("a", 1, 1);
        Unit b = unit("b", 1, 1);
        Unit c = unit("c", 1, 1);
        Band band = band(a, b, c);
        assertThat(band.front()).isSameAs(a);
        assertThat(band.back()).isSameAs(c);
        assertThat(band.at(1)).isSameAs(b);
        assertThat(band.indexOf(c)).isEqualTo(2);
        assertThat(band.indexOf(new UnitId("nope"))).isEqualTo(-1);
        assertThat(band.find(b.id())).contains(b);
    }

    @Test
    void insertShiftsTheUnitsBehind() {
        Unit a = unit("a", 1, 1);
        Unit b = unit("b", 1, 1);
        Unit x = unit("x", 1, 1);
        Band band = band(a, b);
        band.insert(1, x);
        assertThat(band.units()).containsExactly(a, x, b);
        Unit y = unit("y", 1, 1);
        band.insert(band.size(), y);
        assertThat(band.back()).isSameAs(y);
    }

    @Test
    void aSixthUnitDoesNotFit() {
        Band band = band(unit("a", 1, 1), unit("b", 1, 1), unit("c", 1, 1), unit("d", 1, 1), unit("e", 1, 1));
        assertThat(band.isFull()).isTrue();
        assertThatThrownBy(() -> band.add(unit("f", 1, 1))).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void theSameUnitCannotBeAddedTwice() {
        Unit a = unit("a", 1, 1);
        Band band = band(a);
        assertThatThrownBy(() -> band.add(a)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void moveFourToZeroPutsTheUnitInFront() {
        Unit a = unit("a", 1, 1);
        Unit b = unit("b", 1, 1);
        Unit c = unit("c", 1, 1);
        Unit d = unit("d", 1, 1);
        Unit e = unit("e", 1, 1);
        Band band = band(a, b, c, d, e);
        band.move(4, 0);
        assertThat(band.units()).containsExactly(e, a, b, c, d);
        band.move(0, 2);
        assertThat(band.units()).containsExactly(a, b, e, c, d);
        band.move(1, 1);
        assertThat(band.units()).containsExactly(a, b, e, c, d);
    }

    @Test
    void removeClosesTheGap() {
        Unit a = unit("a", 1, 1);
        Unit b = unit("b", 1, 1);
        Unit c = unit("c", 1, 1);
        Band band = band(a, b, c);
        assertThat(band.remove(1)).isSameAs(b);
        assertThat(band.units()).containsExactly(a, c);
        assertThat(band.remove(c)).isTrue();
        assertThat(band.remove(c)).isFalse();
        assertThat(band.units()).containsExactly(a);
    }

    @Test
    void badIndexesAreRejected() {
        Band band = band(unit("a", 1, 1));
        assertThatThrownBy(() -> band.at(1)).isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> band.at(-1)).isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> band.insert(2, unit("b", 1, 1))).isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> band.move(0, 1)).isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> new Band().front()).isInstanceOf(IndexOutOfBoundsException.class);
    }

    @Test
    void aliveSkipsFallenUnits() {
        Unit a = unit("a", 1, 1);
        Unit b = unit("b", 1, 1);
        b.takeDamage(1);
        Band band = band(a, b);
        assertThat(band.alive()).containsExactly(a);
        assertThat(band.anyAlive()).isTrue();
        a.takeDamage(1);
        assertThat(band.anyAlive()).isFalse();
    }

    @Test
    void copyForBattleIsDeep() {
        Unit a = unit("a", 2, 2);
        a.takeDamage(1);
        Band band = band(a);
        Band copy = band.copyForBattle();
        assertThat(copy.front()).isNotSameAs(a);
        assertThat(copy.front().id()).isEqualTo(a.id());
        assertThat(copy.front().damageTaken()).isZero();
        copy.front().takeDamage(2);
        assertThat(a.damageTaken()).isEqualTo(1);
        assertThat(band.copy()).isEqualTo(band).isNotSameAs(band);
        assertThat(band.units()).isUnmodifiable();
    }
}
