export interface SettingValue { en?: string; hi?: string }
export interface SiteData {
  settings: Record<string, SettingValue>;
  stats: { students: number; teachers: number; classes: number; years: number };
}
export interface ClassItem {
  id: number; name: string; description?: string; descriptionHi?: string; subjects?: string;
  sections: string; seats?: number; ageGroup?: string; displayOrder: number;
}
export interface Facility {
  id: number; title: string; titleHi?: string; description?: string; descriptionHi?: string;
  imageUrl?: string; icon?: string;
}
export interface Announcement {
  id: number; title: string; titleHi?: string; body?: string; bodyHi?: string; publishDate: string; pinned: boolean;
}
export interface Testimonial {
  id: number; name: string; role?: string; quote: string; quoteHi?: string; photoUrl?: string;
}
export interface Management {
  id: number; name: string; designation: string; designationHi?: string; photoUrl?: string;
}
export interface AlbumView {
  id: number; title: string; titleHi?: string; category: string; eventDate?: string; coverUrl?: string; photoCount: number;
}
export interface Photo { id: number; url: string; caption?: string }
export interface AlbumDetail { album: AlbumView; photos: Photo[] }
