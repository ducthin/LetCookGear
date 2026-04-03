import { Injectable } from '@angular/core';
import { Client, IMessage } from '@stomp/stompjs';
import { Observable, Subject } from 'rxjs';

import { RealtimeEvent } from '../models/realtime-event.model';

@Injectable({
  providedIn: 'root',
})
export class RealtimeService {
  private client: Client | null = null;
  private readonly eventsSubject = new Subject<RealtimeEvent>();

  readonly events$: Observable<RealtimeEvent> = this.eventsSubject.asObservable();

  connect(): void {
    if (this.client?.active) {
      return;
    }

    this.client = new Client({
      brokerURL: this.resolveBrokerUrl(),
      reconnectDelay: 4000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
    });

    this.client.onConnect = () => {
      this.client?.subscribe('/topic/realtime', (message) => this.handleIncomingMessage(message));
    };

    this.client.activate();
  }

  disconnect(): void {
    if (!this.client) {
      return;
    }

    this.client.deactivate();
    this.client = null;
  }

  private handleIncomingMessage(message: IMessage): void {
    try {
      const parsed = JSON.parse(message.body) as RealtimeEvent;
      if (parsed.type) {
        this.eventsSubject.next(parsed);
      }
    } catch {
      // Ignore malformed realtime payloads.
    }
  }

  private resolveBrokerUrl(): string {
    const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws';
    return `${protocol}://${window.location.host}/ws`;
  }
}