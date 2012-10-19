package org.zanata.webtrans.client.service;

import java.util.Collection;
import java.util.List;

import org.zanata.common.ContentState;
import org.zanata.webtrans.client.events.TransUnitSaveEvent;
import org.zanata.webtrans.shared.model.HasTransUnitId;
import org.zanata.webtrans.shared.model.TransUnitId;
import org.zanata.webtrans.shared.util.FindByTransUnitIdPredicate;
import com.allen_sauer.gwt.log.client.Log;
import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.Singleton;

import static com.google.common.collect.Collections2.filter;

/**
 * @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
@Singleton
public class SaveEventQueue
{
   private List<EventWrapper> eventQueue = Lists.newArrayList();

   public void push(TransUnitSaveEvent event)
   {
      EventWrapper comingEvent = new EventWrapper(event);
      Collection<EventWrapper> previousEvents = filter(eventQueue, Predicates.and(new FindByTransUnitIdPredicate(comingEvent.getId()), NotSavingPredicate.INSTANCE));
      if (previousEvents.isEmpty())
      {
         eventQueue.add(comingEvent);
         Log.info("pushed into queue:" + comingEvent);
         return;
      }
      // there is a previous pending item in it
      EventWrapper prevEvent = previousEvents.iterator().next();
      if (validState(comingEvent, prevEvent))
      {
         replacePreviousEventIfApplicable(comingEvent, prevEvent);
      }
//      Log.info("coming event has invalid state (i.e. old contents does not equal to previous event's new contents). Discard!");
   }

   private static boolean validState(EventWrapper comingEvent, EventWrapper prevEvent)
   {
      return Objects.equal(comingEvent.oldContents, prevEvent.newContents);
   }

   private void replacePreviousEventIfApplicable(EventWrapper comingEvent, EventWrapper prevEvent)
   {
      if (!prevEvent.isSaving && prevEvent.verNumber == comingEvent.verNumber)
      {
         int prevIndex = eventQueue.indexOf(prevEvent);
         eventQueue.set(prevIndex, comingEvent);
         Log.info("replacing old pending event:" + comingEvent);
      }
   }

   public TransUnitSaveEvent getNextPendingForSaving(TransUnitId idToSave)
   {
      Collection<EventWrapper> pendingEvents = filter(eventQueue, Predicates.and(new FindByTransUnitIdPredicate(idToSave), NotSavingPredicate.INSTANCE));
      if (pendingEvents.isEmpty())
      {
         return null;
      }
      else
      {
         EventWrapper wrapper = pendingEvents.iterator().next();
         wrapper.setSaving(true);
         return wrapper.toEvent();
      }
   }

   public void removeSaved(TransUnitSaveEvent saveEvent, int savedVersion)
   {
      EventWrapper saved = new EventWrapper(saveEvent);
      Collection<EventWrapper> sameIdEvents = filter(eventQueue, new FindByTransUnitIdPredicate(saved.getId()));
      if (sameIdEvents.size() == 2)
      {
         EventWrapper pending = Iterables.find(sameIdEvents, NotSavingPredicate.INSTANCE);
         Log.info("found pending save:" + pending);
         pending.setVerNumber(savedVersion);
         Log.info("update version number of pending save:" + pending);
      }
      eventQueue.remove(saved);
   }

   public void removeAllPending(TransUnitId id)
   {
      Log.info("remove all pending for " + id);
      Collection<EventWrapper> pending = filter(eventQueue, new FindByTransUnitIdPredicate(id));
      eventQueue.removeAll(pending);
   }

   protected List<EventWrapper> getEventQueue()
   {
      return ImmutableList.copyOf(eventQueue);
   }

   public boolean hasPending()
   {
      return filter(eventQueue, NotSavingPredicate.INSTANCE).size() > 0;
   }

   public boolean isSaving(TransUnitId transUnitId)
   {
      Collection<EventWrapper> saving = filter(eventQueue, Predicates.and(new FindByTransUnitIdPredicate(transUnitId), HasSavingPredicate.INSTANCE));
      return !saving.isEmpty();
   }

   protected static class EventWrapper implements HasTransUnitId
   {
      private final TransUnitId id;
      private final List<String> newContents;
      private final List<String> oldContents;
      private final ContentState status;
      private int verNumber;
      private boolean isSaving;

      private EventWrapper(TransUnitSaveEvent saveEvent)
      {
         id = saveEvent.getTransUnitId();
         newContents = saveEvent.getTargets();
         oldContents = saveEvent.getOldContents();
         status = saveEvent.getStatus();
         verNumber = saveEvent.getVerNum();
         isSaving = false;
      }

      @Override
      public TransUnitId getId()
      {
         return id;
      }

      public EventWrapper setVerNumber(int verNumber)
      {
         this.verNumber = verNumber;
         return this;
      }

      public EventWrapper setSaving(boolean saving)
      {
         isSaving = saving;
         return this;
      }

      protected boolean isSaving()
      {
         return isSaving;
      }

      public TransUnitSaveEvent toEvent()
      {
         return new TransUnitSaveEvent(newContents, status, id, verNumber, oldContents);
      }

      @Override
      public boolean equals(Object o)
      {
         if (this == o)
         {
            return true;
         }
         if (o == null || getClass() != o.getClass())
         {
            return false;
         }
         EventWrapper that = (EventWrapper) o;
         return Objects.equal(id, that.id) && Objects.equal(verNumber, that.verNumber);
      }

      @Override
      public int hashCode()
      {
         return Objects.hashCode(id, verNumber);
      }

      @Override
      public String toString()
      {
         return Objects.toStringHelper(this).
               add("id", id).
               add("verNumber", verNumber).
               add("isSaving", isSaving).
               add("newContents", newContents).
               toString();
      }
   }

   private static enum NotSavingPredicate implements Predicate<EventWrapper>
   {
      INSTANCE;

      @Override
      public boolean apply(EventWrapper input)
      {
         return !input.isSaving();
      }
   }

   private static enum HasSavingPredicate implements Predicate<EventWrapper>
   {
      INSTANCE;

      @Override
      public boolean apply(EventWrapper input)
      {
         return input.isSaving();
      }
   }
}