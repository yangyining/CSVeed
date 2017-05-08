package org.csveed.bean;

import static org.junit.Assert.assertNull;

import org.csveed.common.Column;
import org.csveed.test.model.BeanSimple;
import org.junit.Test;

public class BeanInstructionsImplTest {

    @Test
    public void propertyNameIsNull() {
        BeanInstructions instructions = new BeanInstructionsImpl(BeanSimple.class);
        assertNull(instructions.getProperties().fromName(new Column("definitelyNotHere")));
    }

}
