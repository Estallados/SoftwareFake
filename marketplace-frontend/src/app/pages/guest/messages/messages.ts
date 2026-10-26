import { Component, computed, signal } from '@angular/core';

export interface Message {
  from: 'me' | 'them';
  text: string;
  time: string;
}

export interface Conversation {
  id: number;
  contact: string;
  initials: string;
  stay: string;
  reservation?: string;
  unread: number;
  messages: Message[];
}

@Component({
  selector: 'app-messages',
  standalone: true,
  templateUrl: './messages.html',
})
export class Messages {
  // TODO: reemplazar por las conversaciones del backend
  conversations = signal<Conversation[]>([]);
  selectedId = signal<number | null>(null);
  draft = signal('');

  /** Conversación seleccionada (o la primera si no hay selección) */
  active = computed(
    () =>
      this.conversations().find((c) => c.id === this.selectedId()) ??
      this.conversations()[0] ??
      null,
  );

  select(id: number) {
    this.selectedId.set(id);
    this.conversations.update((list) => list.map((c) => (c.id === id ? { ...c, unread: 0 } : c)));
  }

  send() {
    const text = this.draft().trim();
    const current = this.active();
    if (!text || !current) return;
    this.conversations.update((list) =>
      list.map((c) =>
        c.id === current.id
          ? { ...c, messages: [...c.messages, { from: 'me' as const, text, time: 'Ahora' }] }
          : c,
      ),
    );
    this.draft.set('');
  }

  last(c: Conversation): Message | undefined {
    return c.messages.at(-1);
  }

  itemClass(c: Conversation) {
    return (
      'flex w-full gap-3 border-b border-slate-100 p-5 text-left ' +
      (this.active()?.id === c.id ? 'bg-emerald-50' : 'hover:bg-slate-50')
    );
  }

  bubbleClass(m: Message) {
    return (
      'max-w-[70%] rounded-2xl px-5 py-3 text-sm leading-6 ' +
      (m.from === 'me' ? 'bg-emerald-700 text-white' : 'border border-slate-200 bg-white')
    );
  }

  timeClass(m: Message) {
    return 'mt-1 text-right text-[10px] ' + (m.from === 'me' ? 'text-emerald-100' : 'text-slate-400');
  }
}
