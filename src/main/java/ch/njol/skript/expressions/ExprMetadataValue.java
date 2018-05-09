/**
 *   This file is part of Skript.
 *
 *  Skript is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Skript is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Skript.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 * Copyright 2011-2017 Peter Güttinger and contributors
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.classes.Converter;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.registrations.Converters;
import ch.njol.skript.util.Utils;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import net.sf.antcontrib.net.Prop;
import org.bukkit.event.Event;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.metadata.Metadatable;
import org.eclipse.jdt.annotation.Nullable;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;

@Name("Metadata")
@Description("Metadata is a way to store temporary data on entities, blocks and more that" +
			"disappears after a server restart.")
@Examples({"set metadata value \"healer\" of player to true",
			"broadcast \"%metadata value \"\"healer\"\" of player%\"",
			"clear metadata value \"healer\" of player"})
@Since("INSERT VERSION")
@SuppressWarnings({"unchecked", "null"})
public class ExprMetadataValue<T> extends SimpleExpression<T> {

	private ExprMetadataValue<?> source;
	@Nullable
	private Expression<String> value;
	@Nullable
	private Expression<Metadatable> holder;
	private Class<T> superType;

	static {
		Skript.registerExpression(ExprMetadataValue.class, Object.class, ExpressionType.PROPERTY,
				"metadata (value|tag) %string% of %metadataholder%",
				"%metadataholder%'[s] metadata (value|tag) %string%"
		);
	}

	public ExprMetadataValue() {
		this(null, (Class<? extends T>) Object.class);
	}

	private ExprMetadataValue(ExprMetadataValue<?> source, Class<? extends T>... types) {
		this.source = source;
		if (source != null) {
			this.value = source.value;
			this.holder = source.holder;
		}
		this.superType = (Class<T>) Utils.getSuperType(types);
	}

	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		holder = (Expression<Metadatable>) exprs[matchedPattern ^ 1];
		value = (Expression<String>) exprs[matchedPattern];
		return true;
	}

	@Override
	@Nullable
	protected T[] get(Event e) {
		Metadatable holder = this.holder.getSingle(e);
		String value = this.value.getSingle(e);
		if (holder == null || value == null) {
			return (T[]) Array.newInstance(superType, 0);
		}
		List<MetadataValue> values = holder.getMetadata(value);
		try {
			return Converters.convertStrictly(new Object[] {values.get(0).value()}, superType);
		} catch (ClassCastException | IndexOutOfBoundsException e1) {
			return (T[]) Array.newInstance(superType, 0);
		}
	}

	@Override
	@Nullable
	public Class<?>[] acceptChange(Changer.ChangeMode mode) {
		if (mode == Changer.ChangeMode.DELETE || mode == Changer.ChangeMode.SET)
			return CollectionUtils.array(Object.class);
		return null;
	}

	@Override
	public void change(Event e, @Nullable Object[] delta, Changer.ChangeMode mode) {
		Metadatable holder = this.holder.getSingle(e);
		String value = this.value.getSingle(e);
		if (value == null || holder == null)
			return;
		switch (mode) {
			case SET:
				holder.setMetadata(value, new FixedMetadataValue(Skript.getInstance(), delta[0]));
				break;
			case DELETE:
				holder.removeMetadata(value, Skript.getInstance());
		}
	}

	@Override
	public boolean isSingle() {
		return true;
	}

	@Override
	public Class<? extends T> getReturnType() {
		return superType;
	}

	@Override
	public <R> Expression<? extends R> getConvertedExpression(Class<R>... to) {
		return new ExprMetadataValue<>(this, to);
	}

	@Override
	public Expression<?> getSource() {
		return source == null ? this : source;
	}

	@Override
	public String toString(@Nullable Event e, boolean debug) {
		return "metadata value " + value.toString(e, debug) + " of " + holder.toString(e, debug);
	}

}