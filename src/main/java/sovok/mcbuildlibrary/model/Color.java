// file: src/main/java/sovok/mcbuildlibrary/model/Color.java
package sovok.mcbuildlibrary.model;

import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@SuperBuilder
public class Color extends BaseNamedEntity {
    // Fields id and name inherited

    /**
     * Default protected constructor required by JPA.
     * Explicitly added to work alongside @SuperBuilder.
     */
    protected Color() { // *** FIX: Explicit protected no-arg constructor ***
        super();
    }

    // SuperBuilder generates the builder and necessary constructor(s) for it.
}