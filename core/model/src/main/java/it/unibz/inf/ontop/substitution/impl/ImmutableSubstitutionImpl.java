package it.unibz.inf.ontop.substitution.impl;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import it.unibz.inf.ontop.model.atom.AtomFactory;
import it.unibz.inf.ontop.model.term.*;
import it.unibz.inf.ontop.substitution.ImmutableSubstitution;
import it.unibz.inf.ontop.substitution.SubstitutionFactory;
import it.unibz.inf.ontop.substitution.Var2VarSubstitution;
import it.unibz.inf.ontop.utils.ImmutableCollectors;

import java.util.AbstractMap;
import java.util.Map;


/**
 * Wrapper above a ImmutableMap<Variable, ImmutableTerm> map.
 */
public class ImmutableSubstitutionImpl<T extends ImmutableTerm> extends AbstractImmutableSubstitutionImpl<T> {

    private final ImmutableMap<Variable, T> map;

    protected ImmutableSubstitutionImpl(ImmutableMap<Variable, ? extends T> substitutionMap,
                                        AtomFactory atomFactory, TermFactory termFactory,
                                        SubstitutionFactory substitutionFactory) {
        super(atomFactory, termFactory, substitutionFactory);
        this.map = (ImmutableMap<Variable, T>) substitutionMap;

        if (substitutionMap.entrySet().stream()
                .anyMatch(e -> e.getKey().equals(e.getValue()))
                ) {
            throw new IllegalArgumentException("Please do not insert entries like t/t in your substitution " +
                    "(for efficiency reasons)\n. Substitution: " + substitutionMap);
        }
    }

    @Override
    public T get(Variable var) {
        return map.get(var);
    }

    @Override
    public ImmutableMap<Variable, T> getImmutableMap() {
        return map;
    }

    @Override
    public boolean isDefining(Variable variable) {
        return map.containsKey(variable);
    }

    @Override
    public ImmutableSet<Variable> getDomain() {
        return map.keySet();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public ImmutableTerm applyToVariable(Variable variable) {
        if (map.containsKey(variable))
            return map.get(variable);
        return variable;
    }

    @Override
    public Var2VarSubstitution getVar2VarFragment() {
        ImmutableMap<Variable, Variable> newMap = map.entrySet().stream()
                .filter(e -> e.getValue() instanceof Variable)
                .map(e -> (Map.Entry<Variable, Variable>) new AbstractMap.SimpleEntry<>(
                        e.getKey(), (Variable) e.getValue()))
                .collect(ImmutableCollectors.toMap());

        return substitutionFactory.getVar2VarSubstitution(newMap);
    }

    @Override
    public ImmutableSubstitution<GroundTerm> getGroundTermFragment() {
        ImmutableMap<Variable, GroundTerm> newMap = map.entrySet().stream()
                .filter(e -> e.getValue() instanceof GroundTerm)
                .map(e -> (Map.Entry<Variable, GroundTerm>) new AbstractMap.SimpleEntry<>(
                        e.getKey(), (GroundTerm) e.getValue()))
                .collect(ImmutableCollectors.toMap());

        return substitutionFactory.getSubstitution(newMap);
    }

    @Override
    public String toString() {
        return Joiner.on(", ").withKeyValueSeparator("/").join(map);
    }

    @Override
    protected ImmutableSubstitution<T> constructNewSubstitution(ImmutableMap<Variable, T> map) {
        return substitutionFactory.getSubstitution(map);
    }

    @Override
    public int hashCode() {
        return map.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof ImmutableSubstitution) {
            return map.equals(((ImmutableSubstitution) o).getImmutableMap());
        }
        else
            return false;
    }


}
