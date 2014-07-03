/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sis.metadata.iso.service;

import java.util.Collection;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.metadata.service.OperationChainMetadata;
import org.opengis.metadata.service.OperationMetadata;
import org.opengis.util.InternationalString;
import org.apache.sis.metadata.iso.ISOMetadata;


/**
 * Operation chain information.
 *
 * @author  Rémi Maréchal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.5
 * @since   0.5
 * @module
 */
@XmlType(name = "SV_OperationChainMetadata_Type", propOrder = {
    "name",
    "description",
    "operation"
})
@XmlRootElement(name = "SV_OperationChainMetadata")
public class DefaultOperationChainMetadata extends ISOMetadata implements OperationChainMetadata {
    /**
     * Serial number for compatibility with different versions.
     */
    private static final long serialVersionUID = 4132508877114835287L;

    /**
     * The name as used by the service for this chain.
     */
    private InternationalString name;

    /**
     * A narrative explanation of the services in the chain and resulting output.
     */
    private InternationalString description;

    /**
     * Information about the operations applied by the chain.
     */
    private Collection<OperationMetadata> operations;

    /**
     * Constructs an initially empty operation chain metadata.
     */
    public DefaultOperationChainMetadata() {
    }

    /**
     * Constructs a new operation chain metadata initialized to the specified values.
     *
     * @param name      The name as used by the service for this chain.
     * @param operation Information about the operations applied by the chain.
     */
    public DefaultOperationChainMetadata(final InternationalString name,
                                         final OperationMetadata operation)
    {
        this.name       = name;
        this.operations = singleton(operation, OperationMetadata.class);
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(OperationChainMetadata)
     */
    public DefaultOperationChainMetadata(final OperationChainMetadata object) {
        super(object);
        if (object != null) {
            this.name        = object.getName();
            this.description = object.getDescription();
            this.operations  = copyCollection(object.getOperations(), OperationMetadata.class);
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultOperationChainMetadata}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultOperationChainMetadata} instance is created using the
     *       {@linkplain #DefaultOperationChainMetadata(OperationChainMetadata) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultOperationChainMetadata castOrCopy(final OperationChainMetadata object) {
        if (object == null || object instanceof DefaultOperationChainMetadata) {
            return (DefaultOperationChainMetadata) object;
        }
        return new DefaultOperationChainMetadata(object);
    }

    /**
     * Returns the name as used by the service for this chain.
     *
     * @return Name as used by the service for this chain.
     */
    @Override
    @XmlElement(name = "name", required = true)
    public InternationalString getName() {
        return name;
    }

    /**
     * Set the name used by the service for this chain.
     *
     * @param newValue The new name used by the service for this chain.
     */
    public void setName(final InternationalString newValue) {
        checkWritePermission();
        name = newValue;
    }

    /**
     * Returns a narrative explanation of the services in the chain and resulting output.
     *
     * @return Narrative explanation of the services in the chain and resulting output, or {@code null} if none.
     */
    @Override
    @XmlElement(name = "description")
    public InternationalString getDescription() {
        return description;
    }

    /**
     * Sets the narrative explanation of the services in the chain and resulting output.
     *
     * @param newValue The new a narrative explanation of the services in the chain and resulting output
     */
    public void setDescription(final InternationalString newValue) {
        checkWritePermission();
        description = newValue;
    }

    /**
     * Returns information about the operations applied by the chain.
     *
     * @return Information about the operations applied by the chain.
     */
    @Override
    @XmlElement(name = "operation", required = true)
    public Collection<OperationMetadata> getOperations() {
        return operations = nonNullCollection(operations, OperationMetadata.class);
    }

    /**
     * Sets the information about the operations applied by the chain.
     *
     * @param newValues The new information about the operations applied by the chain.
     */
    public void setOperations(final Collection<? extends OperationMetadata> newValues) {
        operations = writeCollection(newValues, operations, OperationMetadata.class);
    }
}