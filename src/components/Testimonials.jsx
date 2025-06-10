import { testimonials } from "../constants";

const Testimonials = () => {
  return (
    <div className="mt-20 tracking-wide bg-gray-100 text-black py-10 px-4">
      <h2 className="text-3xl sm:text-5xl lg:text-6xl text-center my-10 lg:my-20 font-bold">
        What People are Saying
      </h2>
      <div className="flex flex-wrap justify-center">
        {testimonials.map((testimonial, index) => (
          <div key={index} className="w-full sm:w-1/2 lg:w-1/3 px-4 py-4">
            <div className="bg-white rounded-xl p-6 shadow-md border border-gray-300 text-base">
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
          </div>
        ))}
      </div>
    </div>
  );
};

export default Testimonials;
