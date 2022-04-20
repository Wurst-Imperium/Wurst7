package net.wurstclient.events;

import com.mojang.bridge.game.GameSession;
import net.wurstclient.event.Event;
import net.wurstclient.event.Listener;

import java.util.ArrayList;

public interface GameSessionEventListener extends Listener {

    void onSessionChange(GameSessionEvent gameSessionEvent);

    class GameSessionEvent extends Event<GameSessionEventListener>
    {
        private final boolean isStarting;

        public GameSessionEvent(boolean isStarting)
        {
            this.isStarting = isStarting;
        }

        @Override
        public void fire(ArrayList<GameSessionEventListener> listeners)
        {
            for(GameSessionEventListener listener : listeners)
                listener.onSessionChange(this);
        }

        @Override
        public Class<GameSessionEventListener> getListenerType()
        {
            return GameSessionEventListener.class;
        }

        public boolean getIsStarting()
        {
            return isStarting;
        }
    }
}