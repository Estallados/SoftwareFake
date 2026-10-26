import { Component, computed, signal } from '@angular/core';

export interface NotificationItem {
  title: string;
  text: string;
  time: string;
  unread: boolean;
}

@Component({
  selector: 'notifications',
  standalone: true,
  templateUrl: './notifications.html',
})
export class Notifications {
  // TODO: reemplazar por las notificaciones del backend
  items = signal<NotificationItem[]>([]);

  unreadCount = computed(() => this.items().filter((i) => i.unread).length);

  markAllRead() {
    this.items.update((list) => list.map((i) => ({ ...i, unread: false })));
    // TODO: mostrar aviso "Todas las notificaciones fueron marcadas como leídas."
  }

  itemClass(item: NotificationItem) {
    return (
      'flex w-full gap-4 border-b border-slate-100 p-6 text-left last:border-0 hover:bg-slate-50 ' +
      (item.unread ? 'bg-emerald-50/40' : '')
    );
  }
}
