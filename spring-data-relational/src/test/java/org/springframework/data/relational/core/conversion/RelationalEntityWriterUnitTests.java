/*
 * Copyright 2017-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.relational.core.conversion;

import static org.assertj.core.api.Assertions.*;

import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.data.annotation.Id;
import org.springframework.data.mapping.PersistentPropertyPath;
import org.springframework.data.mapping.PersistentPropertyPaths;
import org.springframework.data.relational.core.conversion.AggregateChange.Kind;
import org.springframework.data.relational.core.conversion.DbAction.Delete;
import org.springframework.data.relational.core.conversion.DbAction.Insert;
import org.springframework.data.relational.core.conversion.DbAction.InsertRoot;
import org.springframework.data.relational.core.conversion.DbAction.UpdateRoot;
import org.springframework.data.relational.core.mapping.Embedded;
import org.springframework.data.relational.core.mapping.Embedded.OnEmpty;
import org.springframework.data.relational.core.mapping.RelationalMappingContext;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.lang.Nullable;

/**
 * Unit tests for the {@link RelationalEntityWriter}
 *
 * @author Jens Schauder
 * @author Bastian Wilhelm
 * @author Mark Paluch
 */
@RunWith(MockitoJUnitRunner.class)
public class RelationalEntityWriterUnitTests {

	static final long SOME_ENTITY_ID = 23L;
	final RelationalMappingContext context = new RelationalMappingContext();
	final RelationalEntityWriter converter = new RelationalEntityWriter(context);

	final PersistentPropertyPath<RelationalPersistentProperty> listContainerElements = toPath("elements",
			ListContainer.class, context);

	private final PersistentPropertyPath<RelationalPersistentProperty> mapContainerElements = toPath("elements",
			MapContainer.class, context);

	private final PersistentPropertyPath<RelationalPersistentProperty> listMapContainerElements = toPath("maps.elements",
			ListMapContainer.class, context);

	private final PersistentPropertyPath<RelationalPersistentProperty> listMapContainerMaps = toPath("maps",
			ListMapContainer.class, context);

	private final PersistentPropertyPath<RelationalPersistentProperty> noIdListMapContainerElements = toPath(
			"maps.elements", NoIdListMapContainer.class, context);

	private final PersistentPropertyPath<RelationalPersistentProperty> noIdListMapContainerMaps = toPath("maps",
			NoIdListMapContainer.class, context);

	@Test // DATAJDBC-112
	public void newEntityGetsConvertedToOneInsert() {

		SingleReferenceEntity entity = new SingleReferenceEntity(null);
		AggregateChange<SingleReferenceEntity> aggregateChange = //
				new AggregateChange<>(Kind.SAVE, SingleReferenceEntity.class, entity);

		converter.write(entity, aggregateChange);

		assertThat(aggregateChange.getActions()) //
				.extracting(DbAction::getClass, //
						DbAction::getEntityType, //
						DbActionTestSupport::extractPath, //
						DbActionTestSupport::actualEntityType, //
						DbActionTestSupport::isWithDependsOn) //
				.containsExactly( //
						tuple(InsertRoot.class, SingleReferenceEntity.class, "", SingleReferenceEntity.class, false) //
				);
	}

	@Test // DATAJDBC-111
	public void newEntityGetsConvertedToOneInsertByEmbeddedEntities() {

		EmbeddedReferenceEntity entity = new EmbeddedReferenceEntity(null);
		entity.other = new Element(2L);

		AggregateChange<EmbeddedReferenceEntity> aggregateChange = //
				new AggregateChange<>(Kind.SAVE, EmbeddedReferenceEntity.class, entity);

		converter.write(entity, aggregateChange);

		assertThat(aggregateChange.getActions()) //
				.extracting(DbAction::getClass, //
						DbAction::getEntityType, //
						DbActionTestSupport::extractPath, //
						DbActionTestSupport::actualEntityType, //
						DbActionTestSupport::isWithDependsOn) //
				.containsExactly( //
						tuple(InsertRoot.class, EmbeddedReferenceEntity.class, "", EmbeddedReferenceEntity.class, false) //
				);
	}

	@Test // DATAJDBC-112
	public void newEntityWithReferenceGetsConvertedToTwoInserts() {

		SingleReferenceEntity entity = new SingleReferenceEntity(null);
		entity.other = new Element(null);

		AggregateChange<SingleReferenceEntity> aggregateChange = //
				new AggregateChange<>(Kind.SAVE, SingleReferenceEntity.class, entity);

		converter.write(entity, aggregateChange);

		assertThat(aggregateChange.getActions()) //
				.extracting(DbAction::getClass, //
						DbAction::getEntityType, //
						DbActionTestSupport::extractPath, //
						DbActionTestSupport::actualEntityType, //
						DbActionTestSupport::isWithDependsOn) //
				.containsExactly( //
						tuple(InsertRoot.class, SingleReferenceEntity.class, "", SingleReferenceEntity.class, false), //
						tuple(Insert.class, Element.class, "other", Element.class, true) //
				);
	}

	@Test // DATAJDBC-112
	public void existingEntityGetsConvertedToDeletePlusUpdate() {

		SingleReferenceEntity entity = new SingleReferenceEntity(SOME_ENTITY_ID);

		AggregateChange<SingleReferenceEntity> aggregateChange = //
				new AggregateChange<>(Kind.SAVE, SingleReferenceEntity.class, entity);

		converter.write(entity, aggregateChange);

		assertThat(aggregateChange.getActions()) //
				.extracting(DbAction::getClass, //
						DbAction::getEntityType, //
						DbActionTestSupport::extractPath, //
						DbActionTestSupport::actualEntityType, //
						DbActionTestSupport::isWithDependsOn) //
				.containsExactly( //
						tuple(Delete.class, Element.class, "other", null, false), //
						tuple(UpdateRoot.class, SingleReferenceEntity.class, "", SingleReferenceEntity.class, false) //
				);
	}

	@Test // DATAJDBC-112
	public void newReferenceTriggersDeletePlusInsert() {

		SingleReferenceEntity entity = new SingleReferenceEntity(SOME_ENTITY_ID);
		entity.other = new Element(null);

		AggregateChange<SingleReferenceEntity> aggregateChange = new AggregateChange<>(Kind.SAVE,
				SingleReferenceEntity.class, entity);

		converter.write(entity, aggregateChange);

		assertThat(aggregateChange.getActions()) //
				.extracting(DbAction::getClass, //
						DbAction::getEntityType, //
						DbActionTestSupport::extractPath, //
						DbActionTestSupport::actualEntityType, //
						DbActionTestSupport::isWithDependsOn) //
				.containsExactly( //
						tuple(Delete.class, Element.class, "other", null, false), //
						tuple(UpdateRoot.class, SingleReferenceEntity.class, "", SingleReferenceEntity.class, false), //
						tuple(Insert.class, Element.class, "other", Element.class, true) //
				);
	}

	@Test // DATAJDBC-113
	public void newEntityWithEmptySetResultsInSingleInsert() {

		SetContainer entity = new SetContainer(null);
		AggregateChange<RelationalEntityWriterUnitTests.SetContainer> aggregateChange = new AggregateChange<>(Kind.SAVE,
				SetContainer.class, entity);

		converter.write(entity, aggregateChange);

		assertThat(aggregateChange.getActions()) //
				.extracting(DbAction::getClass, //
						DbAction::getEntityType, //
						DbActionTestSupport::extractPath, //
						DbActionTestSupport::actualEntityType, //
						DbActionTestSupport::isWithDependsOn) //
				.containsExactly( //
						tuple(InsertRoot.class, SetContainer.class, "", SetContainer.class, false));
	}

	@Test // DATAJDBC-113
	public void newEntityWithSetResultsInAdditionalInsertPerElement() {

		SetContainer entity = new SetContainer(null);
		entity.elements.add(new Element(null));
		entity.elements.add(new Element(null));

		AggregateChange<SetContainer> aggregateChange = new AggregateChange<>(Kind.SAVE, SetContainer.class, entity);
		converter.write(entity, aggregateChange);

		assertThat(aggregateChange.getActions()).extracting(DbAction::getClass, //
				DbAction::getEntityType, //
				DbActionTestSupport::extractPath, //
				DbActionTestSupport::actualEntityType, //
				DbActionTestSupport::isWithDependsOn) //
				.containsExactly( //
						tuple(InsertRoot.class, SetContainer.class, "", SetContainer.class, false), //
						tuple(Insert.class, Element.class, "elements", Element.class, true), //
						tuple(Insert.class, Element.class, "elements", Element.class, true) //
				);
	}

	@Test // DATAJDBC-113
	public void cascadingReferencesTriggerCascadingActions() {

		CascadingReferenceEntity entity = new CascadingReferenceEntity(null);

		entity.other.add(createMiddleElement( //
				new Element(null), //
				new Element(null)) //
		);

		entity.other.add(createMiddleElement( //
				new Element(null), //
				new Element(null)) //
		);

		AggregateChange<CascadingReferenceEntity> aggregateChange = new AggregateChange<>(Kind.SAVE,
				CascadingReferenceEntity.class, entity);

		converter.write(entity, aggregateChange);

		assertThat(aggregateChange.getActions()).extracting(DbAction::getClass, //
				DbAction::getEntityType, //
				DbActionTestSupport::extractPath, //
				DbActionTestSupport::actualEntityType, //
				DbActionTestSupport::isWithDependsOn) //
				.containsExactly( //
						tuple(InsertRoot.class, CascadingReferenceEntity.class, "", CascadingReferenceEntity.class, false), //
						tuple(Insert.class, CascadingReferenceMiddleElement.class, "other", CascadingReferenceMiddleElement.class,
								true), //
						tuple(Insert.class, CascadingReferenceMiddleElement.class, "other", CascadingReferenceMiddleElement.class,
								true), //
						tuple(Insert.class, Element.class, "other.element", Element.class, true), //
						tuple(Insert.class, Element.class, "other.element", Element.class, true), //
						tuple(Insert.class, Element.class, "other.element", Element.class, true), //
						tuple(Insert.class, Element.class, "other.element", Element.class, true) //
				);
	}

	@Test // DATAJDBC-188
	public void cascadingReferencesTriggerCascadingActionsForUpdate() {

		CascadingReferenceEntity entity = new CascadingReferenceEntity(23L);

		entity.other.add(createMiddleElement( //
				new Element(null), //
				new Element(null)) //
		);

		entity.other.add(createMiddleElement( //
				new Element(null), //
				new Element(null)) //
		);

		AggregateChange<CascadingReferenceEntity> aggregateChange = new AggregateChange<>(Kind.SAVE,
				CascadingReferenceEntity.class, entity);

		converter.write(entity, aggregateChange);

		assertThat(aggregateChange.getActions()).extracting(DbAction::getClass, //
				DbAction::getEntityType, //
				DbActionTestSupport::extractPath, //
				DbActionTestSupport::actualEntityType, //
				DbActionTestSupport::isWithDependsOn) //
				.containsExactly( //
						tuple(Delete.class, Element.class, "other.element", null, false),
						tuple(Delete.class, CascadingReferenceMiddleElement.class, "other", null, false),
						tuple(UpdateRoot.class, CascadingReferenceEntity.class, "", CascadingReferenceEntity.class, false), //
						tuple(Insert.class, CascadingReferenceMiddleElement.class, "other", CascadingReferenceMiddleElement.class,
								true), //
						tuple(Insert.class, CascadingReferenceMiddleElement.class, "other", CascadingReferenceMiddleElement.class,
								true), //
						tuple(Insert.class, Element.class, "other.element", Element.class, true), //
						tuple(Insert.class, Element.class, "other.element", Element.class, true), //
						tuple(Insert.class, Element.class, "other.element", Element.class, true), //
						tuple(Insert.class, Element.class, "other.element", Element.class, true) //
				);
	}

	@Test // DATAJDBC-131
	public void newEntityWithEmptyMapResultsInSingleInsert() {

		MapContainer entity = new MapContainer(null);
		AggregateChange<MapContainer> aggregateChange = new AggregateChange<>(Kind.SAVE, MapContainer.class, entity);

		converter.write(entity, aggregateChange);

		assertThat(aggregateChange.getActions()).extracting(DbAction::getClass, //
				DbAction::getEntityType, //
				DbActionTestSupport::extractPath) //
				.containsExactly( //
						tuple(InsertRoot.class, MapContainer.class, ""));
	}

	@Test // DATAJDBC-131
	public void newEntityWithMapResultsInAdditionalInsertPerElement() {

		MapContainer entity = new MapContainer(null);
		entity.elements.put("one", new Element(null));
		entity.elements.put("two", new Element(null));

		AggregateChange<MapContainer> aggregateChange = new AggregateChange<>(Kind.SAVE, MapContainer.class, entity);
		converter.write(entity, aggregateChange);

		assertThat(aggregateChange.getActions()).extracting(DbAction::getClass, //
				DbAction::getEntityType, //
				this::getMapKey, //
				DbActionTestSupport::extractPath) //
				.containsExactlyInAnyOrder( //
						tuple(InsertRoot.class, MapContainer.class, null, ""), //
						tuple(Insert.class, Element.class, "one", "elements"), //
						tuple(Insert.class, Element.class, "two", "elements") //
				).containsSubsequence( // container comes before the elements
						tuple(InsertRoot.class, MapContainer.class, null, ""), //
						tuple(Insert.class, Element.class, "two", "elements") //
				).containsSubsequence( // container comes before the elements
						tuple(InsertRoot.class, MapContainer.class, null, ""), //
						tuple(Insert.class, Element.class, "one", "elements") //
				);
	}

	@Test // DATAJDBC-183
	public void newEntityWithFullMapResultsInAdditionalInsertPerElement() {

		MapContainer entity = new MapContainer(null);

		entity.elements.put("1", new Element(null));
		entity.elements.put("2", new Element(null));
		entity.elements.put("3", new Element(null));
		entity.elements.put("4", new Element(null));
		entity.elements.put("5", new Element(null));
		entity.elements.put("6", new Element(null));
		entity.elements.put("7", new Element(null));
		entity.elements.put("8", new Element(null));
		entity.elements.put("9", new Element(null));
		entity.elements.put("0", new Element(null));
		entity.elements.put("a", new Element(null));
		entity.elements.put("b", new Element(null));

		AggregateChange<MapContainer> aggregateChange = new AggregateChange<>(Kind.SAVE, MapContainer.class, entity);
		converter.write(entity, aggregateChange);

		assertThat(aggregateChange.getActions()).extracting(DbAction::getClass, //
				DbAction::getEntityType, //
				this::getMapKey, //
				DbActionTestSupport::extractPath) //
				.containsExactlyInAnyOrder( //
						tuple(InsertRoot.class, MapContainer.class, null, ""), //
						tuple(Insert.class, Element.class, "1", "elements"), //
						tuple(Insert.class, Element.class, "2", "elements"), //
						tuple(Insert.class, Element.class, "3", "elements"), //
						tuple(Insert.class, Element.class, "4", "elements"), //
						tuple(Insert.class, Element.class, "5", "elements"), //
						tuple(Insert.class, Element.class, "6", "elements"), //
						tuple(Insert.class, Element.class, "7", "elements"), //
						tuple(Insert.class, Element.class, "8", "elements"), //
						tuple(Insert.class, Element.class, "9", "elements"), //
						tuple(Insert.class, Element.class, "0", "elements"), //
						tuple(Insert.class, Element.class, "a", "elements"), //
						tuple(Insert.class, Element.class, "b", "elements") //
				);
	}

	@Test // DATAJDBC-130
	public void newEntityWithEmptyListResultsInSingleInsert() {

		ListContainer entity = new ListContainer(null);
		AggregateChange<ListContainer> aggregateChange = new AggregateChange<>(Kind.SAVE, ListContainer.class, entity);

		converter.write(entity, aggregateChange);

		assertThat(aggregateChange.getActions()).extracting(DbAction::getClass, //
				DbAction::getEntityType, //
				DbActionTestSupport::extractPath) //
				.containsExactly( //
						tuple(InsertRoot.class, ListContainer.class, ""));
	}

	@Test // DATAJDBC-130
	public void newEntityWithListResultsInAdditionalInsertPerElement() {

		ListContainer entity = new ListContainer(null);
		entity.elements.add(new Element(null));
		entity.elements.add(new Element(null));

		AggregateChange<ListContainer> aggregateChange = new AggregateChange<>(Kind.SAVE, ListContainer.class, entity);
		converter.write(entity, aggregateChange);

		assertThat(aggregateChange.getActions()).extracting(DbAction::getClass, //
				DbAction::getEntityType, //
				this::getListKey, //
				DbActionTestSupport::extractPath) //
				.containsExactlyInAnyOrder( //
						tuple(InsertRoot.class, ListContainer.class, null, ""), //
						tuple(Insert.class, Element.class, 0, "elements"), //
						tuple(Insert.class, Element.class, 1, "elements") //
				).containsSubsequence( // container comes before the elements
						tuple(InsertRoot.class, ListContainer.class, null, ""), //
						tuple(Insert.class, Element.class, 1, "elements") //
				).containsSubsequence( // container comes before the elements
						tuple(InsertRoot.class, ListContainer.class, null, ""), //
						tuple(Insert.class, Element.class, 0, "elements") //
				);
	}

	@Test // DATAJDBC-131
	public void mapTriggersDeletePlusInsert() {

		MapContainer entity = new MapContainer(SOME_ENTITY_ID);
		entity.elements.put("one", new Element(null));

		AggregateChange<MapContainer> aggregateChange = new AggregateChange<>(Kind.SAVE, MapContainer.class, entity);

		converter.write(entity, aggregateChange);

		assertThat(aggregateChange.getActions()) //
				.extracting(DbAction::getClass, //
						DbAction::getEntityType, //
						this::getMapKey, //
						DbActionTestSupport::extractPath) //
				.containsExactly( //
						tuple(Delete.class, Element.class, null, "elements"), //
						tuple(UpdateRoot.class, MapContainer.class, null, ""), //
						tuple(Insert.class, Element.class, "one", "elements") //
				);
	}

	@Test // DATAJDBC-130
	public void listTriggersDeletePlusInsert() {

		ListContainer entity = new ListContainer(SOME_ENTITY_ID);
		entity.elements.add(new Element(null));

		AggregateChange<ListContainer> aggregateChange = new AggregateChange<>(Kind.SAVE, ListContainer.class, entity);

		converter.write(entity, aggregateChange);

		assertThat(aggregateChange.getActions()) //
				.extracting(DbAction::getClass, //
						DbAction::getEntityType, //
						this::getListKey, //
						DbActionTestSupport::extractPath) //
				.containsExactly( //
						tuple(Delete.class, Element.class, null, "elements"), //
						tuple(UpdateRoot.class, ListContainer.class, null, ""), //
						tuple(Insert.class, Element.class, 0, "elements") //
				);
	}

	@Test // DATAJDBC-223
	public void multiLevelQualifiedReferencesWithId() {

		ListMapContainer listMapContainer = new ListMapContainer(SOME_ENTITY_ID);
		listMapContainer.maps.add(new MapContainer(SOME_ENTITY_ID));
		listMapContainer.maps.get(0).elements.put("one", new Element(null));

		AggregateChange<ListMapContainer> aggregateChange = new AggregateChange<>(Kind.SAVE, ListMapContainer.class,
				listMapContainer);

		converter.write(listMapContainer, aggregateChange);

		assertThat(aggregateChange.getActions()) //
				.extracting(DbAction::getClass, //
						DbAction::getEntityType, //
						a -> getQualifier(a, listMapContainerMaps), //
						a -> getQualifier(a, listMapContainerElements), //
						DbActionTestSupport::extractPath) //
				.containsExactly( //
						tuple(Delete.class, Element.class, null, null, "maps.elements"), //
						tuple(Delete.class, MapContainer.class, null, null, "maps"), //
						tuple(UpdateRoot.class, ListMapContainer.class, null, null, ""), //
						tuple(Insert.class, MapContainer.class, 0, null, "maps"), //
						tuple(Insert.class, Element.class, null, "one", "maps.elements") //
				);
	}

	@Test // DATAJDBC-223
	public void multiLevelQualifiedReferencesWithOutId() {

		NoIdListMapContainer listMapContainer = new NoIdListMapContainer(SOME_ENTITY_ID);
		listMapContainer.maps.add(new NoIdMapContainer());
		listMapContainer.maps.get(0).elements.put("one", new NoIdElement());

		AggregateChange<NoIdListMapContainer> aggregateChange = new AggregateChange<>(Kind.SAVE, NoIdListMapContainer.class,
				listMapContainer);

		converter.write(listMapContainer, aggregateChange);

		assertThat(aggregateChange.getActions()) //
				.extracting(DbAction::getClass, //
						DbAction::getEntityType, //
						a -> getQualifier(a, noIdListMapContainerMaps), //
						a -> getQualifier(a, noIdListMapContainerElements), //
						DbActionTestSupport::extractPath) //
				.containsExactly( //
						tuple(Delete.class, NoIdElement.class, null, null, "maps.elements"), //
						tuple(Delete.class, NoIdMapContainer.class, null, null, "maps"), //
						tuple(UpdateRoot.class, NoIdListMapContainer.class, null, null, ""), //
						tuple(Insert.class, NoIdMapContainer.class, 0, null, "maps"), //
						tuple(Insert.class, NoIdElement.class, 0, "one", "maps.elements") //
				);
	}

	private CascadingReferenceMiddleElement createMiddleElement(Element first, Element second) {

		CascadingReferenceMiddleElement middleElement1 = new CascadingReferenceMiddleElement(null);
		middleElement1.element.add(first);
		middleElement1.element.add(second);
		return middleElement1;
	}

	private Object getMapKey(DbAction a) {

		PersistentPropertyPath<RelationalPersistentProperty> path = this.mapContainerElements;

		return getQualifier(a, path);
	}

	private Object getListKey(DbAction a) {

		PersistentPropertyPath<RelationalPersistentProperty> path = this.listContainerElements;

		return getQualifier(a, path);
	}

	@Nullable
	private Object getQualifier(DbAction a, PersistentPropertyPath<RelationalPersistentProperty> path) {

		return a instanceof DbAction.WithDependingOn //
				? ((DbAction.WithDependingOn) a).getQualifiers().get(path) //
				: null;
	}

	static PersistentPropertyPath<RelationalPersistentProperty> toPath(String path, Class source,
			RelationalMappingContext context) {

		PersistentPropertyPaths<?, RelationalPersistentProperty> persistentPropertyPaths = context
				.findPersistentPropertyPaths(source, p -> true);

		return persistentPropertyPaths.filter(p -> p.toDotPath().equals(path)).stream().findFirst().orElse(null);
	}

	@RequiredArgsConstructor
	static class SingleReferenceEntity {

		@Id final Long id;
		Element other;
		// should not trigger own DbAction
		String name;
	}

	@RequiredArgsConstructor
	static class EmbeddedReferenceEntity {

		@Id final Long id;
		@Embedded(onEmpty = OnEmpty.USE_NULL, prefix = "prefix_") Element other;
	}

	@RequiredArgsConstructor
	static class ReferenceWoIdEntity {

		@Id final Long id;
		NoIdElement other;
		// should not trigger own DbAction
		String name;
	}

	@RequiredArgsConstructor
	private static class CascadingReferenceMiddleElement {

		@Id final Long id;
		final Set<Element> element = new HashSet<>();
	}

	@RequiredArgsConstructor
	private static class CascadingReferenceEntity {

		@Id final Long id;
		final Set<CascadingReferenceMiddleElement> other = new HashSet<>();
	}

	@RequiredArgsConstructor
	private static class SetContainer {

		@Id final Long id;
		Set<Element> elements = new HashSet<>();
	}

	@RequiredArgsConstructor
	private static class ListMapContainer {

		@Id final Long id;
		List<MapContainer> maps = new ArrayList<>();
	}

	@RequiredArgsConstructor
	private static class MapContainer {

		@Id final Long id;
		Map<String, Element> elements = new HashMap<>();
	}

	@RequiredArgsConstructor
	private static class ListContainer {

		@Id final Long id;
		List<Element> elements = new ArrayList<>();
	}

	@RequiredArgsConstructor
	private static class Element {
		@Id final Long id;
	}

	@RequiredArgsConstructor
	private static class NoIdListMapContainer {

		@Id final Long id;
		List<NoIdMapContainer> maps = new ArrayList<>();
	}

	@RequiredArgsConstructor
	private static class NoIdMapContainer {

		Map<String, NoIdElement> elements = new HashMap<>();
	}

	@RequiredArgsConstructor
	private static class NoIdElement {
		// empty classes feel weird.
		String name;
	}

}
