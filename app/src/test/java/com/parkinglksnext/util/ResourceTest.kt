package com.parkinglksnext.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ResourceTest {

    @Test fun success_holds_data() {
        assertThat(Resource.Success("hello").data).isEqualTo("hello")
    }

    @Test fun error_holds_message() {
        val err: Resource.Error<String> = Resource.Error("fail")
        assertThat(err.message).isEqualTo("fail")
    }

    @Test fun loading_is_instance_of_Resource() {
        val r: Resource<String> = Resource.Loading()
        assertThat(r).isInstanceOf(Resource::class.java)
    }

    @Test fun sealed_has_three_subtypes() {
        assertThat(Resource::class.sealedSubclasses.size).isAtLeast(3)
    }
}
