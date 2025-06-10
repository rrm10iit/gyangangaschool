import { testimonials } from "../constants";
import { Swiper, SwiperSlide } from "swiper/react";
import { Navigation, Autoplay } from "swiper/modules";
import "swiper/css";
import "swiper/css/navigation";
import "../styles/testimonials.css"; // 👈 import custom CSS

const Testimonials = () => {
  return (
    <div className="mt-20 tracking-wide bg-gray-100 text-black py-10 px-4">
      <h2 className="text-3xl sm:text-5xl lg:text-6xl text-center my-10 lg:my-20 font-bold">
        What People are Saying
      </h2>

      <div className="max-w-6xl mx-auto px-4 relative">
        <Swiper
          modules={[Navigation, Autoplay]}
          navigation
          autoplay={{ delay: 5000, disableOnInteraction: false }}
          spaceBetween={30}
          slidesPerView={1}
          breakpoints={{
            640: { slidesPerView: 1 },
            768: { slidesPerView: 2 },
            1024: { slidesPerView: 3 },
          }}
          loop
        >
          {testimonials.map((testimonial, index) => (
            <SwiperSlide key={index}>
              <div className="bg-white rounded-xl p-6 shadow-md border border-gray-300 text-base mx-2 h-full">
                <p className="mb-6">{testimonial.text}</p>
                <div className="flex items-start">
                  <img
                    className="w-12 h-12 mr-4 rounded-full border border-gray-400"
                    src={testimonial.image}
                    alt={testimonial.user}
                  />
                  <div>
                    <h6 className="font-semibold">{testimonial.user}</h6>
                    <span className="text-sm italic text-gray-600">
                      {testimonial.company}
                    </span>
                  </div>
                </div>
              </div>
            </SwiperSlide>
          ))}
        </Swiper>

        {/* Optional: spacing to avoid buttons overlap */}
        <div className="h-8"></div>
      </div>
    </div>
  );
};

export default Testimonials;
