package org.halalfoursome.kebabeditor.archetype.instance;

public sealed interface NodeArchetype 
    permits 
        ArchetypeInstance,
        UnresolvedArchetype
{
    String archetypeId();
}
