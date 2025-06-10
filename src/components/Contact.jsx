import { CheckCircle2 } from "lucide-react";
import { pricingOptions } from "../constants";

import { Mail, MapPin, Phone } from "lucide-react";

const Contact = () => {
  return (
    <div className="mt-20 px-4 sm:px-8 lg:px-24">
      <h2 className="text-4xl sm:text-5xl lg:text-6xl text-center font-bold my-12 tracking-wide text-orange-700">
        Contact for Admission
      </h2>

      <div className="bg-white border border-gray-200 rounded-2xl shadow-xl p-10 max-w-4xl mx-auto space-y-8 text-lg">
        <div className="flex items-start space-x-4">
          <MapPin className="text-orange-600 w-6 h-6 mt-1" />
          <div>
            <h3 className="font-semibold text-xl mb-1">School Address</h3>
            <p>
              Gyan Ganga School Road<br />
              Rasulabad, Teliarganj,<br />
              Prayagraj, Uttar Pradesh 211002
            </p>
          </div>
        </div>

        <div className="flex items-start space-x-4">
          <Phone className="text-orange-600 w-6 h-6 mt-1" />
          <div>
            <h3 className="font-semibold text-xl mb-1">Phone</h3>
            <p>+91 9756xyz123</p>
          </div>
        </div>

        <div className="flex items-start space-x-4">
          <Mail className="text-orange-600 w-6 h-6 mt-1" />
          <div>
            <h3 className="font-semibold text-xl mb-1">Email</h3>
            <p>gyangangaschool@example.com</p>
          </div>
        </div>

        <div className="text-center pt-8">
          <a
            href="tel:9756xyz123"
            className="inline-block bg-orange-600 text-white text-lg font-semibold px-8 py-3 rounded-xl hover:bg-orange-700 transition duration-200"
          >
            Call for Admission
          </a>
        </div>
      </div>
    </div>
  );
};

export default Contact;

